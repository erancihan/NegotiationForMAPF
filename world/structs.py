from dataclasses import dataclass, asdict


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
