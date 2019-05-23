package main

import (
	"errors"
	"fmt"
	"net/http"
	"os"
	"time"
	"world/handler"

	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
)

type (
	Map map[string]interface{}
)

func main() {
	e := echo.New()             // echo instance
	e.Use(middleware.Recover()) // middleware

	e.HTTPErrorHandler = errorHandler

	redisServer := os.Getenv("REDIS_SERVER")
	if redisServer == "" {
		e.Logger.Fatal(errors.New("env: redis_server is missing"))
	}

	// pool setup
	pool := &redis.Pool{
		MaxIdle:     10,
		MaxActive:   55000,
		IdleTimeout: 240 * time.Second,
		Dial: func() (redis.Conn, error) {
			conn, err := redis.Dial("tcp", redisServer)
			if err != nil {
				e.Logger.Fatal(err)
			}

			return conn, err
		},
		TestOnBorrow: func(c redis.Conn, t time.Time) error {
			_, err := c.Do("PING")
			return err
		},
	}

	// handler setup
	h := &handler.Handler{
		Pool:     pool,
		Ticker:   *time.NewTicker(500 * time.Millisecond),
		Upgrader: &websocket.Upgrader{},
		WorldMap: handler.NewWorldMap(),
	}

	// routes
	e.GET("/", h.Home)
	e.GET("/uuid", h.UKey)
	e.GET("/world/:world_id/:agent_id", h.WorldSocket)
	e.POST("/join/:world_id", h.Join)

	e.File("/test", "res/test.html")

	e.Logger.Fatal(e.Start(":3001"))
}

func errorHandler(err error, c echo.Context) {
	var (
		code = http.StatusInternalServerError
		msg  interface{}
	)

	if he, ok := err.(*echo.HTTPError); ok {
		code = he.Code
		msg = he.Message
		if he.Internal != nil {
			err = fmt.Errorf("%v, %v", err, he.Internal)
		}
	} else {
		msg = http.StatusText(code)
	}

	if _, ok := msg.(string); ok {
		msg = Map{"message": msg}
	}

	// send
	if !c.Response().Committed {
		if c.Request().Method == http.MethodHead {
			err = c.NoContent(code)
		} else {
			err = c.JSON(code, msg)
		}
		if err != nil {
			if code != 404 {
				c.Logger().Error(err)
			}
		}
	}
}
