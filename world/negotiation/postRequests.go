package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"net/http"
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

	for _, agent := range r.Agents {
		_, err = rds.Do("HSET", "world:"+r.WorldID+":notify", agent, "")
	}

	return ctx.NoContent(http.StatusOK)
}

//@POST
func (n *NegotiationHandler) Bid(ctx echo.Context) (err error) {
	return ctx.NoContent(http.StatusOK)
}