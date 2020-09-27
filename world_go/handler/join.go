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
	WorldID   string `json:"world_id" form:"world_id" query:"world_id"`
	Broadcast string `json:"broadcast" form:"broadcast" query:"broadcast"`
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

	// init REDIS position
	_, err = rds.Do("HSET", "world:"+wid+":map", "agent:"+agent.Id, agent.X+":"+agent.Y)
	_, err = rds.Do("HSET", "world:"+wid+":map", agent.X+":"+agent.Y, "agent:"+agent.Id)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// init REDIS broadcast
	_, err = rds.Do("HSET", "world:"+wid+":path", "agent:"+agent.Id, j.Broadcast)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// todo better joined to world response?
	return ctx.NoContent(http.StatusOK)
}

// todo handle agent disconnect
func (h *Handler) Disconnect(ctx echo.Context) (err error) {
	return nil
}
