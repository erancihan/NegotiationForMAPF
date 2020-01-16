package negotiation

import (
	"fmt"
	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
	"net/http"
	"sync"
	"time"
)

var (
	SessionPingPeriod 	= 8 * time.Second
	SessionPongWait 	= 10 * time.Second
	SessionWriteWait	= 2 * time.Second
)

type (
	SessionClient struct {
		conn *websocket.Conn
		updates chan Status
	}
	SessionClientMap struct {
		sync.RWMutex
		m map[*SessionClient]bool
	}
	SessionPool 		struct {
		SessionId string
		SubCount  int
		Clients   *SessionClientMap
	}
	SessionMap 			struct {
		sync.RWMutex
		m map[string]*SessionPool
	}
	Handler struct {
		Pool	 	*redis.Pool
		Upgrader 	*websocket.Upgrader
		Ticker   	time.Ticker
		SessionMap 	*SessionMap
	}
)

func NewSessionMap() *SessionMap {
	return &SessionMap{
		m: make(map[string]*SessionPool),
	}
}

func (t *SessionMap) Load(key string) (*SessionPool, bool) {
	t.RLock()
	defer t.RUnlock()

	result, ok := t.m[key]

	return result, ok
}

func (t *SessionMap) Store(key string, pool *SessionPool) {
	t.Lock()
	defer t.Unlock()

	t.m[key] = pool
}

func (t *SessionMap) Register(key string, client *SessionClient) {
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

func (t *SessionMap) Unregister(key string, client *SessionClient, pool *SessionPool) {
	t.Lock()
	if t.m[key] != nil {
		t.m[key].Clients.Lock()
		defer t.m[key].Clients.Unlock()
	}
	defer t.Unlock()

	pool.SubCount = pool.SubCount - 1
	client.conn.Close()

	if t.m[key] != nil {
		delete(t.m[key].Clients.m, client)
	}
}

func (t *SessionMap) Delete(key string) {
	t.Lock()
	defer t.Unlock()

	delete(t.m, key)
}

func (n *Handler) Socket(ctx echo.Context) error {
	sid := ctx.Param("session_id")

	n.Upgrader.CheckOrigin = func(r *http.Request) bool {
		return true
	}

	ws, err := n.Upgrader.Upgrade(ctx.Response(), ctx.Request(), nil)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	sess, ok := n.SessionMap.Load(sid)

	if !ok { // session with id doesn't exist
		sess = &SessionPool{
			SessionId: sid,
			SubCount:  1,
			Clients: &SessionClientMap{
				m: make(map[*SessionClient]bool),
			},
		}

		n.SessionMap.Store(sid, sess)

		go n.UpdateStatus(ctx, sess)
	} else { // subscribe to session of id
		sess.SubCount = sess.SubCount + 1
	}

	client := &SessionClient{
		conn: ws,
		updates: make(chan Status),
	}

	n.SessionMap.Register(sid, client)
	// session register
	defer func() {
		// session unregister
		_ = n.AgentUnregister(ctx)
		n.SessionMap.Unregister(sid, client, sess)
		if sess.SubCount <= 0 {
			n.Delete(ctx, sid)
			n.SessionMap.Delete(sid)
		}
	}()

	ping := time.NewTicker(SessionPingPeriod)
	go readSessionMessages(ctx, client)
	for {
		select {
		case s := <-client.updates:
			_ = ws.SetWriteDeadline(time.Now().Add(SessionWriteWait))
			err = ws.WriteJSON(s)
			if err != nil { return nil }

		case <-ping.C:
			_ = ws.SetWriteDeadline(time.Now().Add(SessionWriteWait))
			err = ws.WriteMessage(websocket.PingMessage, []byte{})
			if err != nil { return nil }
		}
	}
	return nil
}

func readSessionMessages(ctx echo.Context, client *SessionClient) {
	client.conn.SetReadDeadline(time.Now().Add(SessionPongWait))
	client.conn.SetPongHandler(func(string) error {
		client.conn.SetReadDeadline(time.Now().Add(SessionPongWait))
		return nil
	})

	for {
		_, msg, err := client.conn.ReadMessage()
		if err != nil {
			return
		}

		// TODO get message from socket
		// TODO parse message from socket for agent ID
		// TODO check if agent can join
		// TODO mark agent joined
		// TODO iff all joined, update negotiation state to : {run}
		fmt.Printf("> %s\n", msg)
	}
}

func (n *Handler) AgentUnregister(ctx echo.Context) (err error) {
	rds := n.Pool.Get()
	defer rds.Close()

	// todo remove session_id from world:<world_id>:notify agent:<agent_id>
	//_, err = rds.Do("HDEL", "world:"+wid+":notify", "agent:"+aid)

	return
}

func (n *Handler) Delete(ctx echo.Context, sessionID string)  {
	rds := n.Pool.Get()
	defer rds.Close()

	_, err := rds.Do("DEL", "negotiation:"+sessionID)
	if err != nil { ctx.Logger().Error(err) }
}
