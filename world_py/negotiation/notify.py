from hashlib import md5
from random import shuffle

from redis import Redis

from structs import Notify


def negotiation_notify(data: Notify, redis: Redis):
    data.agents.sort()

    agent_ids = ','.join(data.agents)
    session_id = redis.hget("world:" + data.world_id + ":session_key", agent_ids)
    if session_id is not None:
        return

    session_id: str = md5(agent_ids.encode('utf-8')).hexdigest()

    redis.hset("world:"+data.world_id+":session_keys", agent_ids, session_id)
    redis.hset("world:"+data.world_id+":session_keys", session_id, agent_ids)
    redis.hset("negotiation:"+session_id, "agents", agent_ids)
    redis.hset("negotiation:"+session_id, "agent_count", len(data.agents))
    redis.hset("negotiation:"+session_id, "agent_left", len(data.agents))

    for agent in data.agents:
        redis.hset("world:"+data.world_id+":notify", agent, session_id)
        redis.hset("negotiation:"+session_id, "bid:"+agent, "[]:0")

    shuffle(data.agents)
    bid_order: str = ','.join(data.agents)

    redis.hset("negotiation:"+session_id, "bid_order", bid_order)
    redis.hset("negotiation:"+session_id, "turn", data.agents[0])
    redis.hset("negotiation:"+session_id, "state", "join")

    redis.hincrby("world:"+data.world_id+":", "negotiation_count", "1")

    return
