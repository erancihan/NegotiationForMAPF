package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
	"net/http"
	"strconv"
	"sync"
	"time"
)

var (
	writeWait	=  2 * time.Second
	pongWait	= 10 * time.Second
	pingPeriod 	=  8 * time.Second
)

type (
	Status	 	struct {
		PlayerCount int 	`json:"player_count"`
		Time		int64 	`json:"time"`
	}

	RdsStatus 	struct {
		PlayerCount string 	`redis:"player_count"`
	}

	WorldPool	struct {
		WorldId 	string
		SubCount 	int
		Clients		*ClientMap
	}

	WorldMap	struct {
		sync.RWMutex
		m map[string]*WorldPool
	}

	ClientMap 	struct {
		sync.RWMutex
		m map[*Client]bool
	}

	Client 		struct {
		conn *websocket.Conn
		updates chan Status
	}
)

func NewWorldMap() *WorldMap {
	return &WorldMap{
		m: make(map[string]*WorldPool),
	}
}

func (t *WorldMap) Load(key string) (value *WorldPool, ok bool) {
	t.RLock()
	defer t.RUnlock()

	result, ok := t.m[key]

	return result, ok
}

func (t *WorldMap) Delete(key string) {
	t.Lock()
	defer t.Unlock()

	delete(t.m, key)
}

func (t *WorldMap) Store(key string, value *WorldPool)  {
	t.Lock()
	defer t.Unlock()

	t.m[key] = value
}

func (t *WorldMap) Register(key string, client *Client) {
	t.Lock()
	if t.m[key] != nil {
		t.m[key].Clients.Lock()
		defer t.m[key].Clients.Unlock()
	}
	defer t.Unlock()

	if t.m[key] != nil {
		t.m[key].Clients.m[client] = true
	}
}

func (t *WorldMap) Unregister(key string, client *Client, p *WorldPool) {
	t.Lock()
	if t.m[key] != nil {
		t.m[key].Clients.Lock()
		defer t.m[key].Clients.Unlock()
	}
	defer t.Unlock()

	p.SubCount = p.SubCount - 1
	client.conn.Close()

	if t.m[key] != nil {
		delete(t.m[key].Clients.m, client)
	}
}

func (h *Handler) UpdateStatus(ctx echo.Context, id string, p *WorldPool) {
	rds := h.Pool.Get()
	defer rds.Close()

	for t := range  h.Ticker.C {
		_ = t
		s, err := h.GetStatus(ctx, id, rds, p)
		if err != nil {
			ctx.Logger().Error(err)
		}

		if p.SubCount > 0 {
			for client := range p.Clients.m {
				client.updates <- s
			}
		} else {
			return
		}
	}
}

func (h *Handler) GetStatus(ctx echo.Context, id string, rds redis.Conn, p *WorldPool) (Status, error) {
	ts := RdsStatus{}
	s  := Status{}

	v, err := redis.Values(rds.Do("HGETALL", "world:" + id))
	if err != nil {
		ctx.Logger().Error(err)
		return s, err
	}

	err = redis.ScanStruct(v, &ts)
	if err != nil {
		ctx.Logger().Error(err)
		return s, err
	}

	s.SetStatus(ts, p)
	return s, nil
}

func (s *Status) SetStatus(ts RdsStatus, p *WorldPool)  {
	// set data
	s.PlayerCount, _ = strconv.Atoi(ts.PlayerCount)

	// todo fow stuff here?
	s.Time = time.Now().UnixNano()
}

func (h *Handler) PlayerRegister(ctx echo.Context, p *WorldPool) error {
	wid := ctx.Param("world_id")
	//pid := ctx.Param("player_id")

	rds := h.Pool.Get()
	defer rds.Close()

	_, err := redis.Int64(rds.Do("HINCRBY", "world:" + wid, "player_count", "1"))
	if err != nil {
		ctx.Echo().Logger.Fatal(err)

		return err
	}


	return nil
}

func (h *Handler) PlayerUnregister(ctx echo.Context, p *WorldPool) error {
	wid := ctx.Param("world_id")
	//pid := ctx.Param("player_id")

	rds := h.Pool.Get()
	defer rds.Close()

	_, err := redis.Int64(rds.Do("HINCRBY", "world:" + wid, "player_count", "-1"))
	if err != nil {
		ctx.Echo().Logger.Fatal(err)

		return err
	}

	return nil
}

func (h *Handler) PlayerFOW(ctx echo.Context, p *WorldPool) {

}

func readMsgs(client *Client, c echo.Context)  {
	client.conn.SetReadDeadline(time.Now().Add(pongWait))
	client.conn.SetPongHandler(func(string) error {
		client.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})
	for {
		_, _, err := client.conn.ReadMessage()
		if err != nil {
			return
		}
	}
}

// todo /world/:wid should not create entry on connection
func (h *Handler) WorldSocket(c echo.Context) error {
	id := c.Param("world_id")

	h.Upgrader.CheckOrigin = func(r *http.Request) bool {
		return true
	}
	ws, err := h.Upgrader.Upgrade(c.Response(), c.Request(), nil)
	if err != nil {
		return err
	}

	world, ok := h.WorldMap.Load(id)

	if !ok {	// create new one world
		world = &WorldPool{
			WorldId: id,
			SubCount: 1,
			Clients: &ClientMap{
				m: make(map[*Client]bool),
			},
		}

		h.WorldMap.Store(id, world)
		go h.UpdateStatus(c, id, world)
	} else {	// sub to world with given id
		world.SubCount = world.SubCount + 1
	}

	client := &Client{
		conn: ws,
		updates: make(chan Status),
	}

	h.WorldMap.Register(id, client)
	h.PlayerRegister(c, world)
	defer func() {
		h.PlayerUnregister(c, world)
		h.WorldMap.Unregister(id, client, world)
		if world.SubCount <= 0 {
			h.WorldMap.Delete(id)
		}
	}()

	ping := time.NewTicker(pingPeriod)
	go readMsgs(client, c)
	for {
		select {
		case s := <-client.updates:
			ws.SetWriteDeadline(time.Now().Add(writeWait))
			err = ws.WriteJSON(s)
			if err != nil {
				return nil
			}

		case <-ping.C:
			ws.SetWriteDeadline(time.Now().Add(writeWait))
			if err := ws.WriteMessage(websocket.PingMessage, []byte{}); err != nil {
				return nil
			}
		}
	}
	return nil
}
