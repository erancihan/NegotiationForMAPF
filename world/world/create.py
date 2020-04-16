from redis import Redis


def world_create(data, redis: Redis):
    world_id: str = data["world_id"]

    print("creating", world_id)

    for key in data:
        if key == "world_id":
            continue
        redis.hset(world_id, key, data[key])

    resp = {
        "world_id": world_id
    }

    return resp
