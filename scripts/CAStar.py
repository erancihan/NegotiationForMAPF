import json
from os.path import dirname, join
from queue import PriorityQueue
from typing import List, Union, Tuple, Dict, Set

INF = float("inf")

class Point:
    def __init__(self, x: int, y: int):
        self.x = x
        self.y = y

    def manhattan_dist_to(self, other) -> int:
        return abs(self.x - other.x) + abs(self.y - other.y)

    def key(self) -> str:
        return "{}-{}".format(self.x, self.y)

    def __lt__(self, other):
        return False

    def __str__(self):
        return "Point({}, {})".format(self.x, self.y)


class Agent:
    def __init__(self, start: Union[Point, str], dest: Union[Point, str], agent_name: str = None, agent_class_name: str = None):
        self.start = Point(*start.split("-")) if isinstance(start, str) else start
        self.dest = Point(*dest.split("-")) if isinstance(dest, str) else dest
        self.agent_name = agent_name
        self.agent_class_name = agent_class_name


class World:
    def __init__(self, width: int, height: int, min_path_len: int = None, min_d: int = None, wid: str = None):
        self.width = width
        self.height = height
        self.min_path_len = min_path_len
        self.min_d = min_d
        self.wid = wid


class Scenario:
    def __init__(self, world: World = None, agents: List[Agent] = None, json_file=None):
        self.world: World = world
        self.agents: List[Agent] = agents

        if json_file is not None:
            with open(json_file, 'r') as f:
                file_data = json.loads(f.read())
                self.world = World(**file_data['world'])
                self.agents = [Agent(**a) for a in file_data['agents']]

def a_star(start: Point, dest: Point, config: Scenario, constraints: Dict[str, Set[int]]):
    _t: int = 0
    _open = PriorityQueue()
    _closed = set()
    _g: Dict[str, float] = {}

    # curr -> next
    _links: Dict[str, str] = {}

    def get_neighbours(current: Point, t: int) -> List[Point]:
        hood: List[Point] = []
        for _i in range(9):
            # skip inter-cardinal directions
            if _i % 2 == 0:
                continue

            x: int = current.x + (_i % 3) - 1
            y: int = int(current.y + (_i / 3) - 1)

            if x < 0 or x >= config.world.width:
                continue
            if y < 0 or y >= config.world.height:
                continue
            if _current.key() in constraints:
                if t in constraints[_current.key()]:
                    continue
            hood.append(Point(x, y))

        return hood

    _open.put((start.manhattan_dist_to(dest), start))
    _g[start.key()] = 0.0

    while not _open.empty():
        _current: Point = _open.get()[1]

        if _current.key() in _closed:
            continue
        if _current.key() == dest.key():
            # construct path
            __path = []

            __next = dest.key()
            while __next in _links:
                __path.append(__next)
                __next = _links[__next]
            __path.append(__next)
            __path.reverse()

            return __path

        _closed.add(_current.key())

        neighbours = get_neighbours(_current, _t)
        for neighbour in neighbours:
            if neighbour.key() in _closed:
                continue

            d: float = _g.get(_current.key(), INF) + _current.manhattan_dist_to(neighbour)

            if d < _g.get(neighbour.key(), INF):
                _g[neighbour.key()] = d

                d = d + neighbour.manhattan_dist_to(dest)
                _open.put([d, neighbour])
                _links[neighbour.key()] = _current.key()


def ca_star(config: Scenario):
    constraints = {}
    
    bb = a_star(Point(0, 0), Point(5, 5), config, constraints)
    print(bb)
    pass


if __name__ == '__main__':
    scenario = Scenario(json_file=join(dirname(__file__), '..', 'logs', 'world-scenario.json'))

    print()
