package handler

import (
	"github.com/labstack/echo/v4"
	"net/http"
	"strconv"
	"strings"
)

type Move struct {
	Agent
	WorldID   string `json:"world_id" form:"world_id" query:"world_id"`
	Direction string `json:"direction" form:"direction" query:"direction"`
}

// world_id is string here
// we directly retrieve it in str format from session storage
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

	// DEL map:world:{wid} x:y
	_, err = rds.Do("DEL", "map:world:"+wid, strconv.Itoa(x)+":"+strconv.Itoa(y))
	if err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusInternalServerError)
	}

	switch dir {
	case "N":
		if x-1 >= 0 {
			x = x - 1
		}
	case "S":
		// process world size limit
		x = x + 1
	case "W":
		if y-1 >= 0 {
			y = y - 1
		}
	case "E":
		// process world size limit
		y = y + 1
	default:
	}

	// update data
	agent.X = strconv.Itoa(x)
	agent.Y = strconv.Itoa(y)

	// update REDIS
	_, err = rds.Do("HSET", "map:world:"+wid, "agent:"+agent.Id, agent.X+":"+agent.Y)
	_, err = rds.Do("HSET", "map:world:"+wid, agent.X+":"+agent.Y, "agent:"+agent.Id)
	if err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusInternalServerError)
	}

	return ctx.JSON(http.StatusOK, agent)
}
