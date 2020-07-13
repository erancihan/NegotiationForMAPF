import random
from datetime import datetime
from typing import List

from flask_socketio import emit
from redis import Redis


def get_negotiation_status_dict(session_id: str, redis: Redis):
    status = {
        "agents": "",
        "agent_count": -1,
        "bid_order": "",
        "bids": [],
        "state": "",
        "turn": "",
    }

    session: dict = redis.hgetall(session_id)

    bids = []
    agents: str = session.get("agents")
    for agent in agents.split(','):
        bid = redis.hget(session_id, 'bid:' + agent)
        bids.append([agent, bid])

    status['agents'] = session.get('agents')
    status['agent_count'] = session.get('agent_count')
    status['bid_order'] = session.get('bid_order')
    status['bids'] = bids
    status['state'] = session.get('state')
    status['turn'] = session.get('turn')
    status['turn_count'] = session.get('turn_count')

    return status


def negotiation_socket(world_id: str, session_id: str, agent_id: str, redis: Redis):
    a_id = "agent:" + agent_id
    s_id = 'negotiation:' + session_id

    status = get_negotiation_status_dict(s_id, redis)

    if a_id in status['agents'].split(','):
        emit('invoke_will_join')

    status['time'] = datetime.now().timestamp()
    status['world_id'] = world_id
    status['agent_id'] = agent_id
    status['session_id'] = session_id

    return status

def agent_ready_for_negotiation(message: str, redis: Redis):
    session_id, agent_id = message.split(':')
    a_id = 'agent:' + agent_id
    s_id = 'negotiation:' + session_id

    agents: str = redis.hget(s_id, 'agents')
    joined_agents: str = redis.hget(s_id, 'joined_agents')

    agent_ids = agents.split(',')
    joined_agent_ids = joined_agents.split(',')

    # Remember, no empty string in first index
    # TODO isn't there a better way of doing this?
    if joined_agent_ids[0] == "":
        joined_agent_ids.pop(0)

    if a_id in joined_agent_ids:
        # Agent has already joined, return
        return

    c = len(agent_ids)
    if a_id in agent_ids:
        # Agent can join
        # Update Joined Agent IDs
        joined_agent_ids.append(a_id)
        redis.hset(s_id, 'joined_agents', ','.join(joined_agent_ids))

        # LEGACY: Update number of agents left
        c = redis.hincrby(s_id, 'agents_left', -1)

    if c == 0 and len(agent_ids) == len(joined_agent_ids):
        # All agents have joined
        redis.hset(s_id, 'state', 'run')

def agent_respond_to_make_action(message: str, redis: Redis):
    print('>', message)
    agent_id, bid_type, session_id = message.split('-')
    a_id = 'agent:' + agent_id
    s_id = 'negotiation:' + session_id

    turn: str = redis.hget(s_id, 'turn')

    if turn != a_id:
        return

    print('>', bid_type)

    """
    Marks negotiation done    
    """
    if bid_type == "accept":
        # Agent accepted, conclude negotiation
        redis.hset(s_id, 'state', 'done')

        # Update Currency & Bank information
        # Reveal values in contract
        # - Get PubKeys
        agents: List[str] = redis.hget(s_id, "agents").split(',')
        assert len(agents) == 2

        status = get_negotiation_status_dict(s_id, redis)
        emit('invoke_on_negotiation_done', status)

    """
    Process given Offer
    """
    if bid_type == "offer":
        # Before offer arrives here, it's contract is processed
        # by the agent that makes it.
        # TODO Contract update Rework
        # Retrieve state
        state = redis.hget(s_id, 'state')
        if state != "run":
            return

        order: str = redis.hget(s_id, 'bid_order')

        next_agent = ""
        agents = order.split(',')
        print('>', agents, a_id)
        for i, agent in enumerate(agents):
            # TODO I don't like the way it looks, there should be a more optimal way of doing this
            if agent != a_id:
                continue
            if i+1 < len(agents):
                # There are agents on the queue
                next_agent = agents[i+1]
                continue

            # No more agents on the queue
            # Shuffle while next is not me
            while agents[0] != a_id:
                random.shuffle(agents)

        if next_agent == "":
            # uh oh
            exit(1)

        redis.hset(s_id, 'turn', next_agent)
        redis.hincrby(s_id, 'turn_count', 1)
