package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/gorilla/websocket"
	"time"
)

type (
	Handler struct {
		Pool *redis.Pool
		Upgrader *websocket.Upgrader
		Ticker time.Ticker
		AgentsMap *AgentsMap
	}
)

