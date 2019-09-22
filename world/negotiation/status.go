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
	rds := n.Pool.Get()
	defer rds.Close()

	for t := range n.Ticker.C {
		s, err := n.GetStatus(ctx, rds, pool, t)
		if err != nil {
			ctx.Logger().Fatal(err)
			return
		}

		if pool.SubCount > 0 {
			for client := range pool.Clients.m {
				client.updates <- s
			}
		} else {
			return
		}
	}
}