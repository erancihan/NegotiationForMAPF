package handler

import (
	"github.com/labstack/echo/v4"
	"net/http"
	"time"
)

func (h *Handler) Home(c echo.Context) error {
	t := time.Now()
	d := &Data{}

	return Respond(c, http.StatusOK, "welcome", d, t)
}
