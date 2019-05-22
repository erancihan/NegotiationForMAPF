package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"strconv"
	"time"
)

type (
	Status struct {
		PlayerCount int   `json:"player_count"`
		Time        int64 `json:"time"`
	}

	RdsStatus struct {
		PlayerCount string `redis:"player_count"`
	}
)

func (h *Handler) UpdateStatus(ctx echo.Context, id string, p *WorldPool) {
	rds := h.Pool.Get()
	defer rds.Close()

	for t := range h.Ticker.C {
		_ = t
		s, err := h.GetStatus(ctx, id, rds, p)
		if err != nil {
			ctx.Logger().Error(err)
		}

		if p.SubCount > 0 {
			for client := range p.Clients.m {
				client.updates <- s
			}
		} else {
			return
		}
	}
}

func (h *Handler) GetStatus(ctx echo.Context, id string, rds redis.Conn, p *WorldPool) (Status, error) {
	ts := RdsStatus{}
	s := Status{}

	v, err := redis.Values(rds.Do("HGETALL", "world:"+id))
	if err != nil {
		ctx.Logger().Error(err)
		return s, err
	}

	err = redis.ScanStruct(v, &ts)
	if err != nil {
		ctx.Logger().Error(err)
		return s, err
	}

	s.SetStatus(ts, p)
	return s, nil
}

func (s *Status) SetStatus(ts RdsStatus, p *WorldPool) {
	// set data
	s.PlayerCount, _ = strconv.Atoi(ts.PlayerCount)

	// todo fow stuff here?
	s.Time = time.Now().UnixNano()
}
