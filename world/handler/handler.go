package handler

import (
	"net/http"
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

	World struct {
		 WorldId string `json:"world_id" form:"world_id" query:"agent_id"`
	}
)

// todo register player
func (h *Handler) PlayerRegister(ctx echo.Context, p *WorldPool) error {
	wid := ctx.Param("world_id")

	rds := h.Pool.Get()
	defer rds.Close()

	_, err := redis.Int64(rds.Do("HINCRBY", "world:"+wid+":", "player_count", "1"))
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

	c, err := redis.Int64(rds.Do("HINCRBY", "world:"+wid+":", "player_count", "-1"))
	if err != nil {
		ctx.Echo().Logger.Fatal(err)

		return err
	}

	if c <= 0 {
		_, err := rds.Do(
			"DEL",
			"world:"+wid+":",
			"world:"+wid+":map",
			"world:"+wid+":notify",
			"world:"+wid+":path",
			"world:"+wid+":session_keys",
		)
		if err != nil {
			ctx.Echo().Logger.Fatal(err)
		}
	}

	return nil
}

func (h *Handler) CreateWorld(ctx echo.Context) (err error) {
	world := new(World)
	if err = ctx.Bind(world); err != nil {
		ctx.Logger().Fatal(err)
	}

	wid := world.WorldId

	rds := h.Pool.Get()
	defer rds.Close()

	// todo check if wid exists!!!

	_, err = rds.Do("HSET", "world:"+wid+":", "player_count", "0")
	_, err = rds.Do("HSET", "world:"+wid+":", "world_state", "0")
	if err != nil {
		ctx.Logger().Fatal(err)
	}

	resp := struct {
		WorldId     string `json:"world_id"`
		PlayerCount string `json:"player_count"`
		WorldState  string `json:"world_state"`
	}{
		wid,
		"0",
		"0",
	}

	return ctx.JSON(http.StatusOK, resp)
}

func (h *Handler) WorldList(ctx echo.Context) (err error) {
	rds := h.Pool.Get()
	defer rds.Close()

	iter := 0
	var worlds []string
	for {
		arr, err := redis.Values(rds.Do("SCAN","0", "MATCH", "world:*:"))
		if err != nil {ctx.Echo().Logger.Fatal(err) }

		iter, _ = redis.Int(arr[0], nil)
		world, _ := redis.Strings(arr[1], nil)
		worlds = append(worlds, world...)

		if iter == 0 { break }
	}

	if len(worlds) == 0 {
		worlds = []string{}
	}

	resp := struct {
		Worlds []string `json:"worlds"`
	}{
		worlds,
	}

	return ctx.JSON(http.StatusOK, resp)
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
