from datetime import datetime


def negotiation_socket(world_id: str, session_id: str, agent_id: str):
    return {
        "time": datetime.now().timestamp(),
        "world_id": world_id,
        "agent_id": agent_id,
        "session_id": session_id
    }
