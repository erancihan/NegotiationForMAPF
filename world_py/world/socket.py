from datetime import datetime

from redis import Redis

FieldOfView = 5


def world_socket(world_id: str, agent_id: str, redis: Redis):
    status = {
        "time": datetime.now().timestamp(),
        "world_id": world_id,
        "agent_id": agent_id,
        "pc": 0,
        "world_state": -1,
        "position": "",
        "fov": "",
        "fov_size": FieldOfView,
        "exec_time": -1,
        "time_tick": -1,
    }

    world: dict = redis.hgetall("world:" + world_id + ":")

    status["pc"] = world.get("player_count", 0)
    status["world_state"] = world.get("world_state", -1)

    agent_pos: str = redis.hget("world:" + world_id + ":map", "agent:" + agent_id)
    status["position"] = agent_pos

    pos = agent_pos.split(":")
    ax = int(pos[0])
    ay = int(pos[1])

    # fetch Field of View
    agents = []
    agents.append(["agent:" + agent_id, agent_pos, "-"])
    for i in range(FieldOfView):
        for j in range(FieldOfView):
            ax_s = ax + (j - FieldOfView // 2)
            ay_s = ay + (i - FieldOfView // 2)

            at = str(ax_s) + ":" + str(ay_s)

    status["time_tick"] = world.get("time_tick", -1)

    return status
