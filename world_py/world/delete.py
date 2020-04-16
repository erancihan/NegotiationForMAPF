from redis import Redis


def world_delete(data, redis: Redis):
    world_id: str = data['world_id']

    redis.delete(world_id, world_id+"map", world_id+"notify", world_id+"path", world_id+"session_keys", world_id+"bank")

    resp = {}

    return resp
