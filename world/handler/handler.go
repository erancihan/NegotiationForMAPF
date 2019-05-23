package handler

import (
	"time"

	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
)

const (
	API_VERSION = "v1.0.0"
)

type (
	Handler struct {
		Pool     *redis.Pool
		Upgrader *websocket.Upgrader
		Ticker   time.Ticker
		WorldMap *WorldMap
	}
	Data     struct{}
	Response struct {
		Version  string  `json:"version"`
		ExecTime float64 `json:"exec_time"`
		Msg      string  `json:"msg"`
		*Data    `json:"data"`
	}
	Agent struct {
		Id string `json:"agent_id" form:"agent_id" query:"agent_id"`
		X  string `json:"agent_x" form:"agent_x" query:"agent_x"`
		Y  string `json:"agent_y" form:"agent_y" query:"agent_y"`
	}
)

// todo register player
func (h *Handler) PlayerRegister(ctx echo.Context, p *WorldPool) error {
	wid := ctx.Param("world_id")

	rds := h.Pool.Get()
	defer rds.Close()

	_, err := redis.Int64(rds.Do("HINCRBY", "world:"+wid, "player_count", "1"))
	if err != nil {
		ctx.Echo().Logger.Fatal(err)

		return err
	}

	return nil
}

// todo unregister actions
func (h *Handler) PlayerUnregister(ctx echo.Context, p *WorldPool) error {
	wid := ctx.Param("world_id")
	//pid := ctx.Param("player_id")

	rds := h.Pool.Get()
	defer rds.Close()

	_, err := redis.Int64(rds.Do("HINCRBY", "world:"+wid, "player_count", "-1"))
	if err != nil {
		ctx.Echo().Logger.Fatal(err)

		return err
	}

	return nil
}

func (h *Handler) CreateWorld(ctx echo.Context, p *WorldPool) error {
	wid := ctx.Param("world_id")

	rds := h.Pool.Get()
	defer rds.Close()

	_, err := rds.Do("HSET", "world:"+wid, "player_count", "0")
	_, err = rds.Do("HSET", "world:"+wid, "world_state", "0")
	if err != nil {
		ctx.Echo().Logger.Fatal(err)

		return err
	}

	return nil
}

func (h *Handler) PlayerFOW(ctx echo.Context, p *WorldPool) {

}

func Respond(c echo.Context, code int, msg string, data *Data, st time.Time) error {
	rt := float64(time.Since(st)) / float64(time.Millisecond)

	r := &Response{
		Version:  API_VERSION,
		ExecTime: rt,
		Msg:      msg,
		Data:     data,
	}

	return c.JSON(code, r)
}
