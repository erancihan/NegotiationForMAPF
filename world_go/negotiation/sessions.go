package negotiation

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"net/http"
	"strings"
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
