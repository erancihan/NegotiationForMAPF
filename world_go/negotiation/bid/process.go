package bid

import (
	"fmt"
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"math/rand"
	"strings"
	"time"
)

func (bid *Bid) Process(ctx echo.Context, rds redis.Conn) (err error) {
	turn, err := redis.String(rds.Do("HGET", "negotiation:"+bid.SessionID, "turn"))
	if turn == "agent:"+bid.AgentID {
		if bid.Type == "accept" {
			err = bid.Accept(ctx, rds) // HandleAccept(rds, bid, ctx)
			return
		}

		// register and/or update bid
		//_, err = rds.Do("HSET", "negotiation:"+bid.SessionID, "Bid", bid.Bid)
		if err != nil {
			ctx.Logger().Fatal(err)
		}

		// retrieve state
		state, err := redis.String(rds.Do("HGET", "negotiation:"+bid.SessionID, "state"))
		if err != nil {
			ctx.Logger().Fatal(err)
		}

		if state == "run" {
			// update turn
			order, err := redis.String(rds.Do("HGET", "negotiation:"+bid.SessionID, "bid_order"))
			fmt.Println("debug:order", order)
			if err != nil {
				ctx.Logger().Fatal(err)
			}

			nextAgent := ""
			agents := strings.Split(order, ",")
			for i, agent := range agents {
				if agent == "agent:"+bid.AgentID { // agent who's turn
					if i+1 < len(agents) { // there is more agents left on queue
						nextAgent = agents[i+1]
					} else {
						// new order must be generated
						rand.Seed(time.Now().UnixNano())
						for ok := true; ok; ok = nextAgent == "agent:"+bid.AgentID {
							// don't let the same agent bid over and over
							// keep shuffling until next is different
							rand.Shuffle(len(agents), func(i, j int) { agents[i], agents[j] = agents[j], agents[i] })
							nextAgent = agents[0]
						}
					}
					break
				}
			}
			if nextAgent == "" {
				ctx.Logger().Fatal("next agent cannot be empty")
			}

			_, err = rds.Do("HSET", "negotiation:"+bid.SessionID, "turn", nextAgent)
			if err != nil {
				ctx.Logger().Fatal(err)
			}
		}
	}

	return err
}
