from dataclasses import dataclass, asdict
from typing import List


@dataclass
class _Base:
    def asdict(self):
        return asdict(self)


@dataclass
class Agent(_Base):
    agent_id: str
    agent_x: str
    agent_y: str


@dataclass
class Move(Agent):
    world_id: str
    direction: str
    broadcast: str


@dataclass
class Notify(_Base):
    world_id: str
    agent_id: str
    agents: List[str]
