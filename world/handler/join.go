package handler

import "github.com/labstack/echo/v4"

//todo
// if [world_id status] == nil: create and join
// if [world_id status] == 'passive':
//   if [world_id agent_id] == nil: create & join
//   else: join
// else:
//   if [world_id agent_id] == nil: return denied
//   else: join as spectator -> will observe /world/:wid/:pid

func (h *Handler) Join(ctx echo.Context) error {
	return nil
}
