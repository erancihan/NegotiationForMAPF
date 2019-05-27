package handler

import (
	"github.com/labstack/echo/v4"
	"net/http"
)

//todo
// if [world_id status] == nil: create and join
// if [world_id status] == 'passive':
//   if [world_id agent_id] == nil: create & join
//   else: join
// else:
//   if [world_id agent_id] == nil: return denied
//   else: join as spectator -> will observe /world/:wid/:pid

func (h *Handler) Join(ctx echo.Context) (err error) { // POST
	wid := ctx.Param("world_id")

	rds := h.Pool.Get()
	defer rds.Close()

	agent := new(Agent)
	if err = ctx.Bind(agent); err != nil {
		ctx.Logger().Error(err)
		return
	}

	_, err = rds.Do("HSET", "map:world:"+wid, "agent:"+agent.Id, agent.X+":"+agent.Y)
	_, err = rds.Do("HSET", "map:world:"+wid, agent.X+":"+agent.Y, "agent:"+agent.Id)
	if err != nil {
		ctx.Logger().Error(err)
		return
	}

	// todo better join response?
	return ctx.NoContent(http.StatusOK)
}

// todo handle agent disconnect
func (h *Handler) Disconnect(ctx echo.Context) (err error) {
	wid :=

	return nil
}
