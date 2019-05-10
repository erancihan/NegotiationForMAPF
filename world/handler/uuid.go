package handler

import (
	"github.com/google/uuid"
	"github.com/labstack/echo/v4"
	"net/http"
)

type UKey struct {
	Key string `json:"uuid"`
}

func (h *Handler) UKey(ctx echo.Context) error {
	key, _ := uuid.NewUUID()

	r := &UKey{
		Key: key.String(),
	}

	return ctx.JSON(http.StatusOK, r)
}
