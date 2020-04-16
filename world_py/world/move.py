from redis import Redis

from structs import Move


def move(data: Move, redis: Redis):
    world_id: str = data.world_id

    world_state = redis.hget("world:"+world_id+":", "world_state")

    if world_state == "0":
        return

    x: int = int(data.agent_x)
    y: int = int(data.agent_y)

    if data.direction == "N":
        if y - 1 > 0:
            y = y - 1
    if data.direction == "S":
        # if y is within boundaries
        y = y + 1
    if data.direction == "W":
        if x - 1 > 0:
            x = x - 1
    if data.direction == "E":
        # if x is within boundaries
        x = x + 1

    # check if x:y is occupied
    target = redis.hget("world:"+world_id+":map", str(x)+":"+str(y))
    if target is not None:
        return

    # remove data from source node
    redis.hdel("world:"+world_id+":map", str(x)+":"+str(y))

    # add/update target node
    redis.hset("world:"+world_id+":map", "agent:"+data.agent_id, str(x)+":"+str(y))
    redis.hset("world:"+world_id+":map", str(x)+":"+str(y), "agent:"+data.agent_id)

    # update broadcast
    redis.hset("world:"+world_id+":path", "agent:"+data.agent_id, data.broadcast)

    # update Move Action Count
    redis.hincrby("world:"+world_id+":", "move_action_count", "1")

    return
