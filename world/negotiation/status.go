package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"time"
)

type Status struct {
	AgentCount 	int 	`json:"agent_count"`
	BidOrder 	string	`json:"bid_order"`
	Bids      []string	`json:"bids"`
	State		string	`json:"state"`
	Turn		string	`json:"turn"`
}

func (n *Handler) UpdateStatus(ctx echo.Context, pool *SessionPool) {
	// todo
}