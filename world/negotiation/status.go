package negotiation

import (
	"fmt"
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"strings"
	"time"
)

type (
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
)

func (n *Handler) UpdateStatus(ctx echo.Context, pool *SessionPool) {
	rds := n.Pool.Get()
	defer rds.Close()

	for t := range n.Ticker.C {
		ok, err := redis.Bool(rds.Do("EXISTS", "negotiation:"+ctx.Param("session_id")))
		if err != nil {
			ctx.Logger().Error(err)
			return
		}
		if !ok {
			fmt.Println("ok:", ok)
			return
		}

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

func (n *Handler) GetStatus(ctx echo.Context, rds redis.Conn, pool *SessionPool, st time.Time) (Status, error) {
	sid := ctx.Param("session_id")

	rdsStatus := RdsStatus{}
	status := Status{}

	session, err := redis.Values(rds.Do("HGETALL", "negotiation:"+sid))
	if err != nil { return status, err }

	err = redis.ScanStruct(session, &rdsStatus)
	if err != nil { return status, err }

	// get bids
	var bids [][]string
	agents := strings.Split(rdsStatus.Agents, ",")
	for _, agent := range agents {
		bid, err := redis.String(rds.Do("HGET", "negotiation:"+sid, "bid:"+agent))
		if err != nil {
			ctx.Logger().Error(err)
			return status, err
		}

		bids = append(bids, []string{agent, bid})
	}

	status.AgentCount = rdsStatus.AgentCount
	status.BidOrder   = rdsStatus.BidOrder
	status.Bids       = bids
	status.State      = rdsStatus.State
	status.Turn       = rdsStatus.Turn

	return status, nil
}
