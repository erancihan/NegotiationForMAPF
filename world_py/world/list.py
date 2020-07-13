from typing import Union

from redis import Redis


def list_world_entries(redis: Redis):
    e: Union[bytes, str]

    data = {'worlds': [e if isinstance(e, str) else e.decode('utf-8') for e in redis.scan_iter("world:*:")]}

    return data
