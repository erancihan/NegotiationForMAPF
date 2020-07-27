package negotiation

import (
	crand "crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"fmt"
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"math"
	"math/rand"
	"net/http"
	"strconv"
	"strings"
	"time"
	. "world/negotiation/bid"
)

//@POST
func (n *Handler) Bid(ctx echo.Context) (err error) {
	rds := n.Pool.Get()
	defer rds.Close()

	r := new(Bid)

	_ = r.Process(ctx, rds)

	return ctx.NoContent(http.StatusOK)
}

func (n *Handler) BidProcess(ctx echo.Context, bid *Bid) (err error) {
	rds := n.Pool.Get()
	defer rds.Close()

	turn, err := redis.String(rds.Do("HGET", "negotiation:"+bid.SessionID, "turn"))
	if turn == "agent:"+bid.AgentID {
		if bid.Type == "accept" {
			err = HandleAccept(rds, bid, ctx)
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

func HandleAccept(rds redis.Conn, bid *Bid, ctx echo.Context) (err error) {
	// agent accepted
	// conclude negotiation as one party has accepted
	_, err = rds.Do("HSET", "negotiation:"+bid.SessionID, "state", "done")
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// update bank info
	// fetch agent ids
	wid := ctx.Param("world_id")
	if len(wid) == 0 {
		ctx.Logger().Fatal("world id cannot be empty!")
	}

	contract := Contract{}
	sess, err := redis.Values(rds.Do("HGETALL", "negotiation:"+bid.SessionID))
	if err != nil {
		ctx.Logger().Fatal(err)
	}
	err = redis.ScanStruct(sess, &contract)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// decrypt
	PKa_str, err := redis.String(rds.Do("HGET", "PubKeyVault", contract.A))
	PKb_str, err := redis.String(rds.Do("HGET", "PubKeyVault", contract.B))
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	PKa_b64, err := base64.StdEncoding.DecodeString(PKa_str)
	PKb_b64, err := base64.StdEncoding.DecodeString(PKb_str)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	PKa, err := x509.ParsePKCS1PrivateKey(PKa_b64)
	PKb, err := x509.ParsePKCS1PrivateKey(PKb_b64)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	Ta_b, err := rsa.DecryptPKCS1v15(crand.Reader, PKa, []byte(contract.ETa))
	Tb_b, err := rsa.DecryptPKCS1v15(crand.Reader, PKb, []byte(contract.ETb))
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	Ta, err := strconv.Atoi(string(Ta_b))
	Tb, err := strconv.Atoi(string(Tb_b))
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	ids, _ := redis.String(rds.Do("HGET", "world:"+wid+":session_keys", bid.SessionID))
	idsList := strings.Split(ids, ",")

	fmt.Println(ids)
	var agentBids []BidData
	for _, id := range idsList { // collect agent data
		agentBid, _ := redis.String(rds.Do("HGET", "negotiation:"+bid.SessionID, "bid:"+id))
		agentBidData := strings.Split(agentBid, ":")
		agentToken, _ := strconv.Atoi(agentBidData[1])

		agentBids = append(agentBids, BidData{
			AgentID: id,
			Path:    agentBidData[0],
			Token:   agentToken,
		})
	}
	if len(agentBids) != 2 {
		ctx.Logger().Fatal("there are more than 2 agents! ")
		return
	}

	// compare diffs in last bid tokens, and distribute
	if agentBids == nil {
		ctx.Logger().Fatal("agentBids is nil!")
		return
	}

	if bid.AgentID == contract.A {
		// A accepted
		d := math.Max(float64(Ta - Tb), 0)

		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.A, d) 	// A receives D
		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.B, -1*d) // B pays D
	}
	if bid.AgentID == contract.B {
		// B accepted
		d := math.Max(float64(Ta - Tb), 0)

		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.A, -1*d) // A pays D
		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.B, d) 	// B receives D
	}
	if err != nil {
		fmt.Println("something went wrong")
		ctx.Logger().Fatal(err)
	}

	return
}
