from redis import Redis


def world_listen(world_id, redis: Redis):
    resp = redis.hgetall(world_id)

    return str({str(key, 'UTF-8'): str(resp[key], 'UTF-8') for key in resp})
