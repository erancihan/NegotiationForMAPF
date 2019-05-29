package handler

import (
	"github.com/labstack/echo/v4"
	"net/http"
	"strconv"
	"strings"
)

func (h *Handler) Move(ctx echo.Context) (err error) {
	wid := ctx.Param("world_id")

	rds := h.Pool.Get()
	defer rds.Close()

	agent := new(Agent)
	if err = ctx.Bind(agent); err != nil {
		ctx.Logger().Fatal(err)
		return ctx.NoContent(http.StatusBadRequest)
	}
	x, err := strconv.Atoi(agent.X)
	y, err := strconv.Atoi(agent.Y)

	dir := ctx.Param("direction")
	dir = strings.ToUpper(dir)

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
