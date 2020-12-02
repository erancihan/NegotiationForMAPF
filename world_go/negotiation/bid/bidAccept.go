package bid

import (
	"fmt"
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"math"
	"strconv"
	"strings"
)

func (bid *Bid) Accept(ctx echo.Context, rds redis.Conn) (err error) {
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
	/*
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
	*/
	Ta, err := strconv.Atoi(contract.ETa)
	Tb, err := strconv.Atoi(contract.ETb)
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
		d := math.Max(float64(Ta-Tb), 0)
		if d == 0 {
			// no diff
			return
		}

		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.A, d)    // A receives D
		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.B, -1*d) // B pays D
	}
	if bid.AgentID == contract.B {
		// B accepted
		d := math.Max(float64(Ta-Tb), 0)
		if d == 0 {
			// no diff
			return
		}

		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.A, -1*d) // A pays D
		_, err = rds.Do("HINCRBY", "world:"+wid+":bank", "agent:"+contract.B, d)    // B receives D
	}
	if err != nil {
		fmt.Println("something went wrong", Ta, Tb)
		ctx.Logger().Fatal(err)
	}

	return
}
