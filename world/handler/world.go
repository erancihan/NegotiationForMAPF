package handler

import (
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
	"net/http"
	"sync"
	"time"
)

var (
	writeWait  = 2 * time.Second
	pongWait   = 10 * time.Second
	pingPeriod = 8 * time.Second
)

type (
	WorldMap struct {
		sync.RWMutex
		m map[string]*WorldPool
	}

	WorldPool struct {
		WorldId  string
		SubCount int
		Clients  *ClientMap
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

func (t *WorldMap) Store(key string, value *WorldPool) {
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

func readMsgs(client *Client, c echo.Context) {
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
func (h *Handler) Socket(ctx echo.Context) error {
	wid := ctx.Param("world_id")
	aid := ctx.Param("agent_id")
	id := wid + ":" + aid

	h.Upgrader.CheckOrigin = func(r *http.Request) bool {
		return true
	}
	ws, err := h.Upgrader.Upgrade(ctx.Response(), ctx.Request(), nil)
	if err != nil {
		ctx.Logger().Fatal(err)
		return err
	}

	world, ok := h.WorldMap.Load(id)

	if !ok { // create new one world
		world = &WorldPool{
			WorldId:  id,
			SubCount: 1,
			Clients: &ClientMap{
				m: make(map[*Client]bool),
			},
		}

		h.WorldMap.Store(id, world)

		go h.UpdateStatus(ctx, id, world)
	} else { // sub to world with given id
		world.SubCount = world.SubCount + 1
	}

	client := &Client{
		conn:    ws,
		updates: make(chan Status),
	}

	h.WorldMap.Register(id, client)
	h.PlayerRegister(ctx, world)
	defer func() {
		h.PlayerUnregister(ctx, world)
		h.WorldMap.Unregister(id, client, world)
		if world.SubCount <= 0 {
			h.WorldMap.Delete(id)
		}
	}()

	ping := time.NewTicker(pingPeriod)
	go readMsgs(client, ctx)
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
