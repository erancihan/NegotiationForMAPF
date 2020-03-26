package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"sync"
	"time"
)

type (
	BidStruct struct {
		AgentID   string `json:"agent_id" form:"agent_id" query:"agent_id"`
		SessionID string `json:"session_id" form:"session_id" query:"session_id"`
		Bid       string `json:"bid" form:"bid" query:"bid"`
		Type      string `json:"type" form:"type" query:"type"`
	}
	BidData struct {
		Path    string
		Token   int
		AgentID string
	}
	Status struct {
		AgentCount 	int 		`json:"agent_count"`
		BidOrder 	string		`json:"bid_order"`
		Bids    	[][]string	`json:"bids"`
		State		string		`json:"state"`
		Turn		string		`json:"turn"`
	}

	RdsStatus struct {
		Agents      string  `redis:"agents"`
		AgentCount 	int 	`redis:"agent_count"`
		BidOrder 	string	`redis:"bid_order"`
		State		string	`redis:"state"`
		Turn		string	`redis:"turn"`
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
