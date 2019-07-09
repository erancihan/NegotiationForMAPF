package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
	"sync"
	"time"
)

var (
	writeWait  = 2 * time.Second
	pongWait   = 10 * time.Second
	pingPeriod = 8 * time.Second
)
type (
	Status 					struct {

	}
	Client 					struct {
		conn 	*websocket.Conn
		updates chan Status
	}
	ClientMap 				struct {
		sync.RWMutex
		m map[*Client]bool
	}
	AgentConnectionsPool	struct {
		AgentId 	string
		SubCount 	int
		Clients 	*ClientMap
	}
	AgentConnectionsMap		struct {
		sync.RWMutex
		m map[string]*AgentConnectionsPool
	}
	Handler struct {
		Pool *redis.Pool
		Upgrader *websocket.Upgrader
		Ticker time.Ticker
		AgentConnectionsMap *AgentConnectionsMap
	}
	Data 	 struct {}
	Response struct {}
)

func NewAgentConnectionsMap() *AgentConnectionsMap{
	return &AgentConnectionsMap{
		m: make(map[string]*AgentConnectionsPool),
	}
}

func (t *AgentConnectionsMap) Load(key string) (value *AgentConnectionsPool, ok bool) {
	t.RLock()
	defer t.RUnlock()

	result, ok := t.m[key]

	return result, ok
}

func (t *AgentConnectionsMap) Delete(key string) {
	t.Lock()
	defer t.Unlock()

	delete(t.m, key)
}

func (t *AgentConnectionsMap) Store(key string, value *AgentConnectionsPool) {
	t.Lock()
	defer t.Unlock()

	t.m[key] = value
}

func (t *AgentConnectionsMap) Register(key string, client *Client)  {
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

func (t *AgentConnectionsMap) Unregister(key string, client *Client, p *AgentConnectionsPool)  {
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

func (h *Handler) AgentConnectionSocket(ctx echo.Context) error {
}
