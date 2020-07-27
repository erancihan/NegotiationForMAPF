package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"hash/fnv"
	"math/rand"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"
)

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

	sort.Strings(r.Agents)                  	// sort
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
		_, err = rds.Do("HSET", "negotiation:"+sessionID, "joined_agents", "")
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
