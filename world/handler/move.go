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
	dir := move.Direction
	dir = strings.ToUpper(dir)

	rds := h.Pool.Get()
	defer rds.Close()

	// check if move state
	worldState, err := redis.Int(rds.Do("HGET", "world:"+wid, "world_state"))
	if err != nil {
		ctx.Logger().Fatal(err)
	}
	if worldState == 0 {
		return ctx.NoContent(http.StatusForbidden)
	}

	// DEL world:{wid}:map x:y
	_, err = rds.Do("HDEL", "world:"+wid+":map", agent.X+":"+agent.Y)
	if err != nil {
		ctx.Logger().Fatal(err)
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

	// check if x:y is occupied
	dest, _ := redis.String(rds.Do("HGET", "world:"+wid+":map", xs+":"+ys))
	if len(dest) > 0 {
		return ctx.NoContent(http.StatusForbidden)
	}

	// update data
	agent.X = xs
	agent.Y = ys

	// update REDIS position
	_, err = rds.Do("HSET", "world:"+wid+":map", "agent:"+agent.Id, agent.X+":"+agent.Y)
	_, err = rds.Do("HSET", "world:"+wid+":map", agent.X+":"+agent.Y, "agent:"+agent.Id)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	// update REDIS move
	_, err = rds.Do("HSET", "world:"+wid+":path", "agent:"+agent.Id, move.Broadcast)
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	return ctx.JSON(http.StatusOK, agent)
}
