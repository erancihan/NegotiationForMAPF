package handler

import (
	"github.com/labstack/echo/v4"
	"net/http"
	"time"
)

const API_VERSION = "v1.0.0"

type Home struct {
	Version 	string 	`json:"version"`
	CallTime 	int64	`json:"timestamp"`
}

func (h *Handler) Home(ctx echo.Context) error {
	rt := time.Now().UnixNano()

	r := &Home{
		Version: API_VERSION,
		CallTime: rt,
	}

	return ctx.JSON(http.StatusOK, r)
}
