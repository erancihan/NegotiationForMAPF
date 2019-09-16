package handler

import (
	"github.com/gomodule/redigo/redis"
	"github.com/labstack/echo/v4"
	"strconv"
	"strings"
	"time"
)

var (
	Fov       = 5
	MoveState = [...]string{"halt", "move"}
)

type (
	Status struct {
		AgentId     string     `json:"agent_id"`
		WorldId     string     `json:"world_id"`
		PlayerCount int        `json:"pc"`
		Time        int64      `json:"time"`
		CanMove     int        `json:"can_move"`
		Position    string     `json:"position"`
		Fov         [][]string `json:"fov"`
		FovSize     int        `json:"fov_size"`
		ExecTime    float64    `json:"exec_time"`
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
		s, err := h.GetStatus(ctx, rds, p, t)
		if err != nil {
			ctx.Logger().Fatal(err)
			return
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

func (h *Handler) GetStatus(ctx echo.Context, rds redis.Conn, p *WorldPool, st time.Time) (Status, error) {
	wid := ctx.Param("world_id")
	aid := ctx.Param("agent_id")

	rdsStatus := RdsStatus{}
	status := Status{}

	world, err := redis.Values(rds.Do("HGETALL", "world:"+wid))
	if err != nil {
		return status, err
	}

	err = redis.ScanStruct(world, &rdsStatus)
	if err != nil {
		return status, err
	}

	status.AgentId = aid
	status.WorldId = wid
	status.FovSize = Fov
	status.PlayerCount, _ = strconv.Atoi(rdsStatus.PlayerCount)
	status.CanMove = rdsStatus.WorldState

	agentIsAt, err := redis.String(rds.Do("HGET", "map:world:"+wid, "agent:"+aid))
	if err != nil {
		return status, nil
	}
	status.Position = agentIsAt

	agentpos := strings.Split(agentIsAt, ":")
	ax, _ := strconv.Atoi(agentpos[0])
	ay, _ := strconv.Atoi(agentpos[1])

	// fow
	var agents [][]string
	agents = append(agents, []string{"agent:"+aid, agentIsAt, "-"})
	for i := 0; i < Fov; i++ {
		for j := 0; j < Fov; j++ {
			axS := ax + (j - Fov/2)
			ayS := ay + (i - Fov/2)

			at := strconv.Itoa(axS) + ":" + strconv.Itoa(ayS)
			agentInFov, _ := redis.String(rds.Do("HGET", "map:world:"+wid, at))
			if len(agentInFov) > 0  && !(ax == axS && ay == ayS) {
				path, err := redis.String(rds.Do("HGET", "path:world:"+wid, agentInFov))
				if err != nil { path = "-" }

				agents = append(agents, []string{agentInFov, at, path})
			}
		}
	}
	status.Fov = agents
	// todo return empty array, not null

	status.Time = time.Now().UnixNano()
	status.ExecTime = float64(time.Since(st)) / float64(time.Millisecond)

	return status, nil
}
