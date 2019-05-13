package handler

import "github.com/gomodule/redigo/redis"

type (
	Handler struct {
		Pool *redis.Pool
	}
)

