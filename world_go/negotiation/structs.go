package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"sync"
	"time"
)

type (
	Status struct {
		AgentCount int        `json:"agent_count"`
		BidOrder   string     `json:"bid_order"`
		Bids       [][]string `json:"bids"`
		State      string     `json:"state"`
		Turn       string     `json:"turn"`
	}
	RdsStatus struct {
		Agents     string `redis:"agents"`
		AgentCount int    `redis:"agent_count"`
		BidOrder   string `redis:"bid_order"`
		State      string `redis:"state"`
		Turn       string `redis:"turn"`
	}
	SessionClient struct {
		conn    *websocket.Conn
		updates chan Status
	}
	SessionClientMap struct {
		sync.RWMutex
		m map[*SessionClient]bool
	}
	SessionPool struct {
		SessionId string
		SubCount  int
		Clients   *SessionClientMap
	}
	SessionMap struct {
		sync.RWMutex
		m map[string]*SessionPool
	}
	Handler struct {
		Pool       *redis.Pool
		Upgrader   *websocket.Upgrader
		Ticker     time.Ticker
		SessionMap *SessionMap
	}
)
