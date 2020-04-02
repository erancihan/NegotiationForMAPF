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
	Contract struct {
		Ox string `json:"Ox" form:"Ox" query:"Ox" redis:"Ox"`
		X string `json:"x" form:"x" query:"x" redis:"x"`
		ETa string `json:"ETa" form:"ETa" query:"ETa" redis:"ETa"`
		A string `json:"a" form:"a" query:"a" redis:"a"`
		ETb string `json:"ETb" form:"ETb" query:"ETb" redis:"ETb"`
		B string `json:"b" form:"b" query:"b" redis:"b"`
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
