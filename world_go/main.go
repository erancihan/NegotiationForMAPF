package main

import (
	"fmt"
	"net/http"
	"os"
	"time"
	"world/handler"
	"world/negotiation"

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
		redisServer = "localhost:6379"
		e.Logger.Warn("env: redis_server is missing, using " + redisServer)
	}
	fmt.Print("REDIS_SERVER := ", redisServer)

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
	// Ticker -> ticks every X
	h := &handler.Handler{
		Pool:     pool,
		Ticker:   *time.NewTicker(10 * time.Millisecond),
		Upgrader: &websocket.Upgrader{},
		WorldMap: handler.NewWorldMap(),
	}

	// negotiation session handler
	n := &negotiation.Handler{
		Pool:       pool,
		Ticker:     *time.NewTicker(250 * time.Millisecond),
		Upgrader:   &websocket.Upgrader{},
		SessionMap: negotiation.NewSessionMap(),
	}

	// routes
	e.GET("/", h.Home)
	e.GET("/uuid", h.UKey)
	e.GET("/worlds", h.WorldList) // has direct handler
	e.GET("/world/:world_id/:agent_id", h.Socket)
	//e.GET("/world/:key", h.Socket)
	//e.POST("/world/create", h.CreateWorld)
	e.POST("/move", h.Move)
	e.POST("/join", h.Join) // has direct handler

	// negotiation routes
	e.GET("/negotiation/:world_id/:session_id/:agent_id", n.Socket)
 	//e.GET("/negotiation/session/:key", n.Socket)
	e.POST("/negotiation/sessions", n.Sessions) // has direct handler
	e.POST("/negotiation/notify", n.Notify)
	e.POST("/negotiation/bid", n.Bid) // is not called!

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
