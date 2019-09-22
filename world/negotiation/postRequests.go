package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"
)

//@POST
func (n *NegotiationHandler) Sessions(ctx echo.Context) (err error) {
	r := new(struct{
		WorldID string `json:"world_id" form:"world_id" query:"world_id"`
		AgentID	string `json:"agent_id" form:"agent_id" query:"agent_id"`
	})

	rds := n.Pool.Get()
	defer rds.Close()

	if err = ctx.Bind(r); err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusBadRequest)
	}

	sessions, err := redis.String(rds.Do("HGET", "world:"+r.WorldID+":notify", "agent:"+r.AgentID))
	if err != nil { sessions = "" }

	response := struct {
		Sessions string `json:"sessions"`
	}{
		Sessions: sessions,
	}

	return ctx.JSON(http.StatusOK, response)
}

//@POST
func (n *NegotiationHandler) Notify(ctx echo.Context) (err error) {
	r := new(struct{
		WorldID string `json:"world_id" form:"world_id" query:"world_id"`
		AgentID	string `json:"agent_id" form:"agent_id" query:"agent_id"`
		Agents  []string `json:"agents" form:"agents" query:"agents"`
	})

	rds := n.Pool.Get()
	defer rds.Close()

	if err = ctx.Bind(r); err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusBadRequest)
	}

	sort.Strings(r.Agents) // sort
	agentIDs := strings.Join(r.Agents, ",") // join

	// check if key exists
	sessionID, err := redis.String(rds.Do("HGET", "world:"+r.WorldID+":session_keys", agentIDs))
	if err != nil {
		sessionID = strconv.FormatInt(time.Now().UnixNano(), 10)

		_, err = rds.Do("HSET", "world:"+r.WorldID+":session_keys", agentIDs, sessionID)
		if err != nil { ctx.Logger().Fatal() }
		_, err = rds.Do("HSET", "world:"+r.WorldID+":session_keys", sessionID, agentIDs)
		if err != nil { ctx.Logger().Fatal() }

		for _, agent := range r.Agents {
			// world:{world_id}:notify agent:{agent_id} {session_id}
			_, err = rds.Do("HSET", "world:"+r.WorldID+":notify", agent, sessionID)
		}
	}

	return ctx.NoContent(http.StatusOK)
}

//@POST
func (n *NegotiationHandler) Bid(ctx echo.Context) (err error) {
	return ctx.NoContent(http.StatusOK)
}