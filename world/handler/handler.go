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
	Handler 	struct {
		Pool 		*redis.Pool
		Upgrader	*websocket.Upgrader
		Ticker 		time.Ticker
		WorldMap	*WorldMap
	}
	Data 		struct {}
	Response 	struct {
		Version 	string 	`json:"version"`
		ExecTime 	float64 `json:"exec_time"`
		Msg			string  `json:"msg"`
		*Data		  		`json:"data"`
	}
)

func Respond(c echo.Context, code int, msg string, data *Data, st time.Time) error {
	rt := float64(time.Since(st)) / float64(time.Millisecond)

	r := &Response{
		Version: 	API_VERSION,
		ExecTime:	rt,
		Msg:		msg,
		Data: 		data,
	}

	return c.JSON(code, r)
}