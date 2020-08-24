from redis import Redis


def world_listen(world_id, redis: Redis):
    resp = redis.hgetall(world_id)
    resp["negotiation_count"] = len(redis.keys("negotiation:*"))

    return resp
    # return str({str(key, 'UTF-8'): str(resp[key], 'UTF-8') for key in resp})
