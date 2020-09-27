package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"net/http"
	"strconv"
	"strings"
)

type Move struct {
	Agent
	WorldID   string `json:"world_id" form:"world_id" query:"world_id"`
	Direction string `json:"direction" form:"direction" query:"direction"`
	Broadcast string `json:"broadcast" form:"broadcast" query:"broadcast"`
}

func (h *Handler) Move(ctx echo.Context) (err error) {
	move := new(Move)
	if err = ctx.Bind(move); err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusBadRequest)
	}

	agent := move.Agent
	x, err := strconv.Atoi(agent.X)
	y, err := strconv.Atoi(agent.Y)

	wid := move.WorldID

	_aid := "agent:" + agent.Id
	_wid := "world:" + wid + ":"
	_map := _wid + "map"
	_path := _wid + "path"
	_curr := agent.X + ":" + agent.Y

	dir := move.Direction
	dir = strings.ToUpper(dir)

	rds := h.Pool.Get()
	defer rds.Close()

	// check if move state
	worldState, err := redis.Int(rds.Do("HGET", _wid, "world_state"))
	if err != nil {
		ctx.Logger().Fatal(err)
	}
	if worldState == 0 {
		return ctx.NoContent(http.StatusForbidden)
	}

	switch dir {
	case "N":
		if y-1 >= 0 {
			y = y - 1
		}
	case "S":
		// process world size limit
		y = y + 1
	case "W":
		if x-1 >= 0 {
			x = x - 1
		}
	case "E":
		// process world size limit
		x = x + 1
	default:
	}

	xs := strconv.Itoa(x)
	ys := strconv.Itoa(y)

	_next := xs + ":" + ys

	// check if next x:y is occupied
	dest, _ := redis.String(rds.Do("HGET", _map, _next))
	if len(dest) > 0 {
		return ctx.NoContent(http.StatusForbidden)
	}

	// DEL world:{wid}:map x:y
	_, err = rds.Do("HDEL", _map, _curr)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// update data
	agent.X = xs
	agent.Y = ys

	// update REDIS position
	_, err = rds.Do("HSET", _map, _aid, _next)
	_, err = rds.Do("HSET", _map, _next, _aid)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// update REDIS move
	_, err = rds.Do("HSET", _path, _aid, move.Broadcast)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// increment REDIS move action count
	_, err = rds.Do("HINCRBY", _wid, "move_action_count", "1")
	if err != nil {
		ctx.Logger().Error("Move: HINCRBY move_action_count 1 > ", err)
	}

	return ctx.JSON(http.StatusOK, agent)
}
