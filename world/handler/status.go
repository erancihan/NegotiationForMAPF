package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"strconv"
	"strings"
	"time"
)

var (
	Fow = 5
	MoveState = [...]string{"halt", "move"}
)

type (
	Status struct {
		Id          string `json:"id"`
		PlayerCount int    `json:"player_count"`
		Time        int64  `json:"time"`
		CanMove	    int    `json:"can_move"`
		Position    string `json:"position"`
		Fow         [][]string `json:"fow"`
	}

	RdsStatus struct {
		PlayerCount string `redis:"player_count"`
		WorldState  int    `redis:"world_state"`
	}
)

func (h *Handler) UpdateStatus(ctx echo.Context, id string, p *WorldPool) {
	rds := h.Pool.Get()
	defer rds.Close()

	for t := range h.Ticker.C {
		_ = t
		s, err := h.GetStatus(ctx, rds, p)
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

func (h *Handler) GetStatus(ctx echo.Context, rds redis.Conn, p *WorldPool) (Status, error) {
	wid := ctx.Param("world_id")
	aid := ctx.Param("agent_id")

	rdsStatus := RdsStatus{}
	status := Status{}

	world, err := redis.Values(rds.Do("HGETALL", "world:" + wid))
	if err != nil {
		ctx.Logger().Error(err)
		return status, err
	}

	err = redis.ScanStruct(world, &rdsStatus)
	if err != nil {
		ctx.Logger().Error(err)
		return status, err
	}

	status.Id = aid
	status.PlayerCount, _ = strconv.Atoi(rdsStatus.PlayerCount)
	status.CanMove = rdsStatus.WorldState

	agent, err := redis.String(rds.Do("HGET", "map:world:" + wid, "agent:" + aid))
	if err != nil {
		ctx.Logger().Error(err)
		return status, nil
	}
	status.Position = agent

	agentpos := strings.Split(agent, ":")
	ax, _ := strconv.Atoi(agentpos[0])
	ay, _ := strconv.Atoi(agentpos[1])
	// fow
	var agents [][]string
	for i := 0; i < Fow; i++ {
		for j := 0; j < Fow; j++ {
			ax_s := strconv.Itoa(ax + j - (Fow/2))
			ay_s := strconv.Itoa(ay + i - (Fow/2))

			at := ax_s + ":" + ay_s
			agentFow, _ := redis.String(rds.Do("HGET", "map:world:" + wid, at))
			if len(agentFow) > 0 && ax_s != ay_s {
				agents = append(agents, []string{agentFow, at})
			}
		}
	}
	status.Fow = agents

	status.Time = time.Now().UnixNano()

	return status, nil
}
