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

type Join struct {
	Agent
	WorldID string   `json:"world_id" form:"world_id" query:"world_id"`
}

func (h *Handler) Join(ctx echo.Context) (err error) { // POST
	j := new(Join)
	if err = ctx.Bind(j); err != nil {
		ctx.Logger().Fatal(err)
		return
	}
	wid := j.WorldID
	agent := j.Agent

	rds := h.Pool.Get()
	defer rds.Close()

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
	return nil
}
