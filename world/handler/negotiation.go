package handler

import (
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
	SessionStatus 		struct {

	}
	SessionClient struct {
		conn *websocket.Conn
		updates chan SessionStatus
	}
	SessionClientMap struct {
		sync.RWMutex
		m map[*SessionClient]bool
	}
	SessionPool 		struct {
		SessionId	string
		SubCount	int
		SessClients *SessionClientMap
	}
	SessionMap 			struct {
		sync.RWMutex
		m map[string]*SessionPool
	}
	NegotiationHandler 	struct {
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
		t.m[key].SessClients.Lock()
		defer t.m[key].SessClients.Unlock()
	}
	defer t.Unlock()

	if t.m[key] != nil {
		t.m[key].SessClients.m[client] = true
	}
}

func (t *SessionMap) Unregister(key string, client *SessionClient, pool *SessionPool) {
	t.Lock()
	if t.m[key] != nil {
		t.m[key].SessClients.Lock()
		defer t.m[key].SessClients.Unlock()
	}
	defer t.Unlock()

	pool.SubCount = pool.SubCount - 1
	client.conn.Close()

	if t.m[key] != nil {
		delete(t.m[key].SessClients.m, client)
	}
}

func (t *SessionMap) Delete(key string) {
	t.Lock()
	defer t.Unlock()

	delete(t.m, key)
}

func (n *NegotiationHandler) Socket(ctx echo.Context) error {
	sid := ctx.Param("session_id")
	// todo creation of sessions should be regulated

	n.Upgrader.CheckOrigin = func(r *http.Request) bool {
		return true
	}

	ws, err := n.Upgrader.Upgrade(ctx.Response(), ctx.Request(), nil)
	if err != nil {
		ctx.Logger().Fatal()
	}

	sess, ok := n.SessionMap.Load(sid)

	if !ok { // session with id doesn't exist
		sess = &SessionPool{
			SessionId: sid,
			SubCount:  1,
			SessClients: &SessionClientMap{
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
		updates: make(chan SessionStatus),
	}

	n.SessionMap.Register(sid, client)
	// session register
	defer func() {
		// session unregister
		n.SessionMap.Unregister(sid, client, sess)
		if sess.SubCount <= 0 {
			n.SessionMap.Delete(sid)
		}
	}()

	ping := time.NewTicker(SessionPingPeriod)
	go readSessionMessages(ctx, client)
	for {
		select {
		case s := <-client.updates:
			ws.SetWriteDeadline(time.Now().Add(SessionWriteWait))
			err = ws.WriteJSON(s)
			if err != nil {
				return nil
			}

		case <-ping.C:
			ws.SetWriteDeadline(time.Now().Add(SessionWriteWait))
			if err := ws.WriteMessage(websocket.PingMessage, []byte{}); err != nil {
				return nil
			}
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
		_, _, err := client.conn.ReadMessage()
		if err != nil {
			return
		}
	}
}

func (n *NegotiationHandler) UpdateStatus(ctx echo.Context, pool *SessionPool) {
	// todo
}

func (n *NegotiationHandler) Sessions(ctx echo.Context) (err error) { // GET
	return ctx.NoContent(http.StatusOK)
}

func (n *NegotiationHandler) Notify(ctx echo.Context) (err error) { // POST
	return ctx.NoContent(http.StatusOK)
}

func (n *NegotiationHandler) Bid(ctx echo.Context) (err error) { // POST
	return ctx.NoContent(http.StatusOK)
}
