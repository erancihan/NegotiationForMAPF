package main

import (
	"comms/handler"
	"errors"
	"github.com/gorilla/websocket"
	"os"
	"time"

	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
)

func main() {
	e := echo.New()             // echo instance
	e.Use(middleware.Recover()) // middleware
	e.Use(middleware.CORSWithConfig(middleware.CORSConfig{
		AllowOrigins: []string{"http://localhost:3000", "http://localhost:8080"},
		AllowHeaders: []string{
			echo.HeaderAccessControlAllowOrigin,
			echo.HeaderOrigin,
			echo.HeaderAccept,
			echo.HeaderContentType},
	}))

	e.HTTPErrorHandler = errorHandler

	redisServer := os.Getenv("REDIS_SERVER")
	if redisServer == "" {
		e.Logger.Fatal(errors.New("env: REDIS_SERVER undefined"))
	}

	// pool setup
	pool := &redis.Pool{
		MaxIdle: 10,
		MaxActive: 55000,
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
		Pool: pool,
		Ticker:   *time.NewTicker(250 * time.Millisecond),
		Upgrader: &websocket.Upgrader{},
		AgentConnectionsMap: handler.NewAgentConnectionsMap(),
	}

	// routes
	e.GET("/", h.Home)

	e.Logger.Fatal(e.Start(":3002"))
}

func errorHandler(err error, ctx echo.Context) {
	// todo implement
}
