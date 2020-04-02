package negotiation

import (
	crand "crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"fmt"
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"hash/fnv"
	"math"
	"math/rand"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"
)

//@POST returns session list for given world and agent
func (n *Handler) Sessions(ctx echo.Context) (err error) {
	r := new(struct {
		WorldID string `json:"world_id" form:"world_id" query:"world_id"`
		AgentID string `json:"agent_id" form:"agent_id" query:"agent_id"`
	})

	rds := n.Pool.Get()
	defer rds.Close()

	if err = ctx.Bind(r); err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusBadRequest)
	}

	sessionList, err := redis.String(rds.Do("HGET", "world:"+r.WorldID+":notify", "agent:"+r.AgentID))
	if err != nil {
		sessionList = ""
	}

	sessions := strings.Split(sessionList, ",")

	response := struct {
		Sessions []string `json:"sessions"`
	}{
		Sessions: sessions,
	}

	return ctx.JSON(http.StatusOK, response)
}

//@POST
func (n *Handler) Notify(ctx echo.Context) (err error) {
	r := new(struct {
		WorldID string   `json:"world_id" form:"world_id" query:"world_id"`
		AgentID string   `json:"agent_id" form:"agent_id" query:"agent_id"`
		Agents  []string `json:"agents" form:"agents" query:"agents"`
	})

	rds := n.Pool.Get()
	defer rds.Close()

	if err = ctx.Bind(r); err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusBadRequest)
	}

	sort.Strings(r.Agents)                  // sort
	agentIDs := strings.Join(r.Agents, ",") // join

	// shuffle agent order
	rand.Seed(time.Now().UnixNano())
	rand.Shuffle(len(r.Agents), func(i, j int) { r.Agents[i], r.Agents[j] = r.Agents[j], r.Agents[i] })

	// check if key exists
	sessionID, err := redis.String(rds.Do("HGET", "world:"+r.WorldID+":session_keys", agentIDs))
	if err != nil {
		// key does not exist
		h := fnv.New64()
		_, _ = h.Write([]byte(agentIDs))
		sessionID = strconv.FormatUint(h.Sum64(), 36)

		_, err = rds.Do("HSET", "world:"+r.WorldID+":session_keys", agentIDs, sessionID)
		_, err = rds.Do("HSET", "world:"+r.WorldID+":session_keys", sessionID, agentIDs)
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "agents", agentIDs)
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "agent_count", len(r.Agents))
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "agents_left", len(r.Agents))

		for _, agent := range r.Agents {
			// path, _ := redis.String(rds.Do("HGET", "world:"+r.WorldID+":path", agent)) // initial bid is path
			// world:{world_id}:notify agent:{agent_id} {session_id}
			_, err = rds.Do("HSET", "world:"+r.WorldID+":notify", agent, sessionID)
			_, err = rds.Do("HSET", "negotiation:"+sessionID, "bid:"+agent, "[]:0")
		}

		// initial order in which agents will bid
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "bid_order", strings.Join(r.Agents, ","))
		// indicates which agent's turn is it to bid
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "turn", r.Agents[0])
		// indicates state of negotiation => negotiation:<session_id> state <{join|run|done}>
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "state", "join")

		if err != nil {
			ctx.Logger().Fatal(err)
		}

		_, err = rds.Do("HINCRBY", "world:"+r.WorldID+":", "negotiation_count", "1")
		if err != nil {
			ctx.Logger().Error("Notify: HINCRBY world negotiation_count 1 > ", err)
		}
	}

	return ctx.NoContent(http.StatusOK)
}

//@POST
func (n *Handler) Bid(ctx echo.Context) (err error) {
	r := new(BidStruct)

	_ = n.BidProcess(ctx, r)

	return ctx.NoContent(http.StatusOK)
}

func (n *Handler) BidProcess(ctx echo.Context, bid *BidStruct) (err error) {
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

func HandleAccept(rds redis.Conn, bid *BidStruct, ctx echo.Context) (err error) {
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
	err = redis.ScanStruct(sess, contract)
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

	Ta_b, err := rsa.DecryptPKCS1v15(crand.Reader, PKa, []byte(contract.ETa))
	Tb_b, err := rsa.DecryptPKCS1v15(crand.Reader, PKb, []byte(contract.ETb))

	Ta, err := strconv.Atoi(string(Ta_b))
	Tb, err := strconv.Atoi(string(Tb_b))

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
