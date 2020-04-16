from redis import Redis


def list_world_entries(redis: Redis):
    e: bytes

    data = {'worlds': [e.decode('utf-8') for e in redis.scan_iter("world:*:")]}

    return data
