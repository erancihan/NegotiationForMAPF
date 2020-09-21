import os
import json
from os.path import dirname, realpath, join, basename
from dataclasses import dataclass, asdict
from glob import glob
from typing import Dict, List
import xlsxwriter
import copy

DEBUG = True

# TODO get opponent id


if DEBUG:
    def debug(*args):
        print(*args)
else:
    def debug(*args):
        pass


class _Base:
    def __init__(self):
        super().__init__()

    def asdict(self):
        return asdict(self)

class NegotiationAction(_Base):
    timestamp: str
    negotiation_id: str
    bidder: str
    A: str
    T_a: str
    B: str
    T_b: str
    Ox: str
    x: str

    def __init__(self):
        super().__init__()
        self.timestamp          = None
        self.negotiation_id     = None
        self.bidder             = None
        self.A                  = None
        self.T_a                = None
        self.B                  = None
        self.T_b                = None
        self.Ox                 = None
        self.x                  = None

    def __str__(self):
        return "{{ timestamp: {}, negotiation_id: {}, bidder: {}, A: {}, Ta: {}, B: {}, Tb: {}, Ox: {}, x: {} }}" \
            .format(
            self.timestamp,
            self.negotiation_id,
            self.bidder,
            self.A,
            self.T_a,
            self.B,
            self.T_b,
            self.Ox,
            self.x
        )

    __repr__ = __str__

class NegotiationSummaries(_Base):
    negotiation_id: str                 = None
    opponent_id: str                    = None
    own_path_before: str                = None
    own_path_before_len: str            = None
    own_path_after: str                 = None
    own_path_after_len: str             = None
    own_token_balance_diff: int         = None
    duration: str                       = None
    conflict_location: str              = None
    is_win: str                         = None
    actions: List[NegotiationAction]

    def __init__(self):
        super().__init__()
        self.actions = []
        self.negotiation_id                 = None
        self.opponent_id                    = None
        self.own_path_before                = None
        self.own_path_before_len            = None
        self.own_path_after                 = None
        self.own_path_after_len             = None
        self.own_token_balance_diff         = None
        self.duration                       = None
        self.conflict_location              = None
        self.is_win                         = None

    def __str__(self, padd=0):
        _str = "{\n"
        _str = _str + "".rjust(padd + 4) + "negotiation_id: {},\n".format(self.negotiation_id)
        _str = _str + "".rjust(padd + 4) + "actions: [\n"
        for action in self.actions:
            _str = _str + "".rjust(padd + 7) + action.__str__() + ",\n"
        _str = _str + "],\n".rjust(padd + 7)
        _str = _str + "}\n".rjust(padd + 3)

        return _str

class Negotiation(_Base):
    timestamp: str
    session_id: str
    agent_ids: List
    conflict_location: str

    def __init__(self):
        super().__init__()
        self.timestamp = None
        self.session_id = None
        self.conflict_location = None
        self.agent_ids = []

    def __str__(self, padd=0):
        _str = "{\n"
        _str = _str + "".rjust(padd + 1) + "session_id: {},\n".format(self.session_id)
        _str = _str + "".rjust(padd + 1) + "agent_ids: {},\n".format(self.agent_ids)
        _str = _str + "".rjust(padd + 1) + "conflict_location: {},\n".format(self.conflict_location)
        _str = _str + "}\n".rjust(padd + 1)

        return _str

    __repr__ = __str__

class Agent(_Base):
    agent_id: str
    agent_name: str
    starting_point: str
    destination: str
    planned_initial_path: str
    planned_initial_path_length: str
    taken_path: str
    taken_path_length: str
    negotiation_count: str
    negotiations_won: str
    negotiations_lost: str
    token_count_initial: str
    token_count_final: str
    negotiations: Dict[str, NegotiationSummaries]

    def __init__(self):
        super().__init__()
        self.negotiations = {}
        self.agent_id                        = None
        self.agent_name                      = None
        self.starting_point                  = None
        self.destination                     = None
        self.planned_initial_path            = None
        self.planned_initial_path_length     = None
        self.taken_path                      = None
        self.taken_path_length               = None
        self.negotiation_count               = None
        self.negotiations_won                = None
        self.negotiations_lost               = None
        self.token_count_initial             = None
        self.token_count_final               = None

    def __str__(self, padd=0):
        _str = "{\n"
        _str = _str + "".rjust(padd + 1) + "agent_id: {},\n".format(self.agent_id)
        _str = _str + "".rjust(padd + 1) + "agent_name: {},\n".format(self.agent_name)
        _str = _str + "".rjust(padd + 1) + "starting_point: {},\n".format(self.starting_point)
        _str = _str + "".rjust(padd + 1) + "destination: {},\n".format(self.destination)
        _str = _str + "".rjust(padd + 1) + "planned_initial_path: {},\n".format(self.planned_initial_path)
        _str = _str + "".rjust(padd + 1) + "planned_initial_path_length: {},\n".format(self.planned_initial_path_length)
        _str = _str + "".rjust(padd + 1) + "taken_path: {},\n".format(self.taken_path)
        _str = _str + "".rjust(padd + 1) + "taken_path_length: {},\n".format(self.taken_path_length)
        _str = _str + "".rjust(padd + 1) + "negotiation_count: {},\n".format(self.negotiation_count)
        _str = _str + "".rjust(padd + 1) + "negotiations_won: {},\n".format(self.negotiations_won)
        _str = _str + "".rjust(padd + 1) + "negotiations_lost: {},\n".format(self.negotiations_lost)
        _str = _str + "".rjust(padd + 1) + "token_count_initial: {},\n".format(self.token_count_initial)
        _str = _str + "".rjust(padd + 1) + "token_count_final: {},\n".format(self.token_count_final)
        _str = _str + "".rjust(padd + 1) + "negotiations: {\n"
        for n_key in self.negotiations:
            _str = _str + " '".rjust(padd + 4) + n_key + "': " + self.negotiations[n_key].__str__(padd + 2)
        _str = _str + "}\n".rjust(padd + 3)
        _str = _str + "},\n".rjust(padd + 2)

        return _str

    __repr__ = __str__

class World(_Base):
    def __str__(self):
        return super().__str__()

class ExcelData(_Base):
    world: World
    agents: Dict[str, Agent]
    negotiations: Dict[str, Negotiation]

    def __init__(self):
        super().__init__()
        self.world = World()
        self.agents = {}
        self.negotiations = {}

    def __str__(self):
        _str = "World: " + self.world.__str__() + ",\n"
        _str = _str + "Agents: {\n"
        for a_key in self.agents:
            _str = _str + " '".rjust(3) + a_key + "': " + self.agents[a_key].__str__(4)
        _str = _str + "},\n"
        _str = _str + "Negotiations: {\n"
        for n_key in self.negotiations:
            _str = _str + " '".rjust(3) + n_key + "': " + self.negotiations[n_key].__str__(4)
        _str = _str + "}"

        return _str

def parse_agent_info_log(file_path: str, data_dict: ExcelData):
    agent_id = basename(file_path).replace('AGENT-INFO-', '').replace('.log', '')

    with open(file_path, 'r') as log:
        line: str
        for line in log:
            timestamp, entry = line.strip().split(';')
            data: dict = json.loads(entry)

            if data['step'] == 'JOIN':
                if agent_id not in data_dict.agents:
                    data_dict.agents[agent_id] = Agent()
                    data_dict.agents[agent_id].agent_id = agent_id
                data_dict.agents[agent_id].token_count_initial = data['token_count']

            if data['step'] == 'MOVE':
                pass

            if data['step'] == 'LEAVE':
                data_dict.agents[agent_id].token_count_final = data['token_count']

def parse_agent_negotiation_log(file_path: str, data_dict: ExcelData):
    agent_id = basename(file_path).replace('AGENT-NEGOTIATIONS-', '').replace('.log', '')

    with open(file_path, 'r') as log:
        line: str
        for line in log:
            timestamp, entry = line.strip().split(';')
            data: dict = json.loads(entry)

            if data['name'] == 'PRE':
                session_id = data['session_id']
                if session_id not in data_dict.negotiations:
                    data_dict.negotiations[session_id] = Negotiation()
                    data_dict.negotiations[session_id].timestamp = timestamp
                    data_dict.negotiations[session_id].session_id = session_id
                    data_dict.negotiations[session_id].conflict_location = data['conflict_location']
                data_dict.negotiations[session_id].agent_ids.append(agent_id)

                if session_id not in data_dict.agents[agent_id].negotiations:
                    data_dict.agents[agent_id].negotiations[session_id]                         = NegotiationSummaries()
                    data_dict.agents[agent_id].negotiations[session_id].negotiation_id          = session_id
                    data_dict.agents[agent_id].negotiations[session_id].own_path_before         = data['path']
                    data_dict.agents[agent_id].negotiations[session_id].own_path_before_len     = len(data['path'].split(','))
                    data_dict.agents[agent_id].negotiations[session_id].conflict_location       = data['conflict_location']
                    data_dict.agents[agent_id].negotiations[session_id].own_token_balance_diff  = int(data['token'])

            if data['name'] == 'OFFER':
                action = NegotiationAction()
                action.timestamp = timestamp
                action.bidder = data['turn']
                action.negotiation_id = data['contract']['sess_id']
                action.Ox   = data['contract']['Ox']
                action.x    = data['contract']['x']
                action.A    = data['contract']['A']
                action.T_a  = data['contract']['ETa']
                action.B    = data['contract']['B']
                action.T_b  = data['contract']['ETb']

                data_dict.agents[agent_id].negotiations[data['contract']['sess_id']].actions.append(action)
            if data['name'] == 'ACCEPT':
                pass
            if data['name'] == 'POST':
                session_id = data['session_id']

                data_dict.agents[agent_id].negotiations[session_id].own_path_after      = data['path']
                data_dict.agents[agent_id].negotiations[session_id].own_path_after_len  = len(data['path'].split(','))

                tb = data_dict.agents[agent_id].negotiations[session_id].own_token_balance_diff
                data_dict.agents[agent_id].negotiations[session_id].own_token_balance_diff  = int(data['token']) - tb
                data_dict.agents[agent_id].negotiations[session_id].is_win                  = data['is_win']

def parse_world_log(file_path: str, data_dict: ExcelData):
    with open(file_path, 'r') as log:
        line: str
        for line in log:
            timestamp, entry = line.strip().split(';')
            data: dict = json.loads(entry)

            # debug(timestamp, data)
            if data['name'] == 'CREATE':
                pass
            if data['name'] == 'AGENT_JOIN':
                data_dict.agents[data['agent_id']].agent_name                   = data['agent_name']
                data_dict.agents[data['agent_id']].starting_point               = data['start']
                data_dict.agents[data['agent_id']].destination                  = data['dest']
                data_dict.agents[data['agent_id']].planned_initial_path         = data['path']
                data_dict.agents[data['agent_id']].planned_initial_path_length  = data['path_len']
            if data['name'] == 'JOIN':
                pass
            if data['name'] == 'BROADCAST':
                pass
            if data['name'] == 'NEGOTIATE':
                if data['state'] == 'BEGIN':
                    pass
                if data['state'] == 'DONE':
                    pass
            if data['name'] == 'MOVE':
                pass
            if data['name'] == 'DONE':
                pass
            if data['name'] == 'LEAVE':
                data_dict.agents[data['agent_id']].taken_path           = data['path']
                data_dict.agents[data['agent_id']].taken_path_length    = data['path_len']
                data_dict.agents[data['agent_id']].negotiation_count    = data['negoC']
                data_dict.agents[data['agent_id']].negotiations_won     = data['winC']
                data_dict.agents[data['agent_id']].negotiations_lost    = data['loseC']
                data_dict.agents[data['agent_id']].negotiation_count    = data['negoC']

def run():
    for world_folder in glob(join(dirname(__file__), '..', 'logs', 'WORLD-*')):
        if os.path.exists(join(world_folder, '.parsed')):
            continue

        debug(' ┬', world_folder)

        data_dict = ExcelData()

        log_files = glob(join(world_folder, '*.log'))
        for i, log_file in enumerate(log_files):
            file_name = basename(log_file)

            if i < len(log_files) - 1:
                debug(' ├', file_name)
            else:
                debug(' └', file_name)

            if 'AGENT-INFO-' in file_name:
                parse_agent_info_log(log_file, data_dict)
            if 'AGENT-NEGOTIATIONS-' in file_name:
                parse_agent_negotiation_log(log_file, data_dict)
            if 'WORLD' in file_name:
                parse_world_log(log_file, data_dict)

        debug(data_dict)
        debug()

        # create world workbook
        wwb = xlsxwriter.Workbook(join(world_folder, 'World.xlsx'))

        wws_agents = wwb.add_worksheet('Agents')
        wws_agents_r = 0
        wws_agents_c = 0
        wws_agents_h = ['id', 'name', 'starting point', 'destination', 'planned path', 'planned path len', 'taken path', 'taken path len', 'negotiation count', 'sum win', 'sum lose', 'initial token count', 'final token count']
        for item in wws_agents_h:
            wws_agents.write(wws_agents_r, wws_agents_c, item)
            wws_agents_c += 1
        wws_agents_r += 1

        wws_agent: Agent
        for wws_agent_key in data_dict.agents:
            wws_agent = data_dict.agents[wws_agent_key]

            wws_agents.write(wws_agents_r, 0, wws_agent.agent_id)
            wws_agents.write(wws_agents_r, 1, wws_agent.agent_name)
            wws_agents.write(wws_agents_r, 2, wws_agent.starting_point)
            wws_agents.write(wws_agents_r, 3, wws_agent.destination)
            wws_agents.write(wws_agents_r, 4, wws_agent.planned_initial_path)
            wws_agents.write(wws_agents_r, 5, wws_agent.planned_initial_path_length)
            wws_agents.write(wws_agents_r, 6, wws_agent.taken_path)
            wws_agents.write(wws_agents_r, 7, wws_agent.taken_path_length)
            wws_agents.write(wws_agents_r, 8, wws_agent.negotiation_count)
            wws_agents.write(wws_agents_r, 9, wws_agent.negotiations_won)
            wws_agents.write(wws_agents_r, 10, wws_agent.negotiations_lost)
            wws_agents.write(wws_agents_r, 11, wws_agent.token_count_initial)
            wws_agents.write(wws_agents_r, 12, wws_agent.token_count_final)

            wws_agents_r += 1

        wws_negotiations = wwb.add_worksheet('Negotiations')

        wws_negotiations_r = 0
        wws_negotiations_c = 0
        wws_negotiations_h = ['timestamp', 'negotiation_id', 'agents', 'conflict_location']
        for item in wws_negotiations_h:
            wws_negotiations.write(wws_negotiations_r, wws_negotiations_c, item)
            wws_negotiations_c += 1
        wws_negotiations_r += 1

        wws_negotiation: Negotiation
        for negotiation_key in data_dict.negotiations:
            wws_negotiation = data_dict.negotiations[negotiation_key]

            wws_negotiations.write(wws_negotiations_r, 0, wws_negotiation.timestamp)
            wws_negotiations.write(wws_negotiations_r, 1, wws_negotiation.session_id)
            wws_negotiations.write(wws_negotiations_r, 2, ",".join(wws_negotiation.agent_ids))
            wws_negotiations.write(wws_negotiations_r, 3, wws_negotiation.conflict_location)

            wws_negotiations_r += 1

        wwb.close()
        del wwb

        for agent_key in data_dict.agents:
            aws_agent: Agent
            aws_agent = data_dict.agents[agent_key]

            awb = xlsxwriter.Workbook(join(world_folder, 'Agent-{}.xlsx'.format(agent_key)))

            # BEGIN AGENT WORKSHEET NEGOTIATION SUMMARIES
            aws_nego_sum_r = 0
            aws_nego_sum_c = 0
            aws_nego_sum_h = ['negotiation_id', 'opponent id', 'own_path_before', 'own_path_after', 'own_path_before_len', 'own_path_after_len', 'duration', 'conflict location', 'scaled time', 'own_token_balance_diff', 'is win', 'is lose']
            aws_nego_sum = awb.add_worksheet('Negotiation Summaries')

            for item in aws_nego_sum_h:
                aws_nego_sum.write(aws_nego_sum_r, aws_nego_sum_c, item)
                aws_nego_sum_c += 1
            aws_nego_sum_r += 1

            for nego_key in aws_agent.negotiations:
                aws_agent_nego_sum: NegotiationSummaries = aws_agent.negotiations[nego_key]

                # find opponent
                opponent = copy.copy(data_dict.negotiations[nego_key].agent_ids)
                opponent.remove(agent_key)
                data_dict.agents[agent_key].negotiations[nego_key].opponent_id = opponent[0]

                aws_nego_sum.write(aws_nego_sum_r, 0, aws_agent_nego_sum.negotiation_id)
                aws_nego_sum.write(aws_nego_sum_r, 1, aws_agent_nego_sum.opponent_id)
                aws_nego_sum.write(aws_nego_sum_r, 2, aws_agent_nego_sum.own_path_before)
                aws_nego_sum.write(aws_nego_sum_r, 3, aws_agent_nego_sum.own_path_after)
                aws_nego_sum.write(aws_nego_sum_r, 4, aws_agent_nego_sum.own_path_before_len)
                aws_nego_sum.write(aws_nego_sum_r, 5, aws_agent_nego_sum.own_path_after_len)
                aws_nego_sum.write(aws_nego_sum_r, 6, aws_agent_nego_sum.duration)
                aws_nego_sum.write(aws_nego_sum_r, 7, aws_agent_nego_sum.conflict_location)
                aws_nego_sum.write(aws_nego_sum_r, 8, '')
                aws_nego_sum.write(aws_nego_sum_r, 9, aws_agent_nego_sum.own_token_balance_diff)
                aws_nego_sum.write(aws_nego_sum_r, 10, aws_agent_nego_sum.is_win)
                aws_nego_sum.write(aws_nego_sum_r, 11, "1" if aws_agent_nego_sum.is_win == "0" else "0")

                aws_nego_sum_r += 1
            # END

            # BEGIN AGENT NEGOTIATION ACTIONS
            aws_nego_act_r = 0
            aws_nego_act_c = 0
            aws_nego_act_h = ['timestamp', 'negotiation_id', 'bidder', 'A', 'T_a', 'B', 'T_b', 'Ox', 'x', 'scaled time']
            aws_nego_act = awb.add_worksheet('Negotiation Actions')

            for item in aws_nego_act_h:
                aws_nego_act.write(aws_nego_act_r, aws_nego_act_c, item)
                aws_nego_act_c += 1
            aws_nego_act_r += 1

            for nego_key in aws_agent.negotiations:
                nego_data: NegotiationSummaries
                nego_data = aws_agent.negotiations[nego_key]

                for action in nego_data.actions:
                    aws_nego_act.write(aws_nego_act_r, 0, action.timestamp)
                    aws_nego_act.write(aws_nego_act_r, 1, action.negotiation_id)
                    aws_nego_act.write(aws_nego_act_r, 2, action.bidder)
                    aws_nego_act.write(aws_nego_act_r, 3, action.A)
                    aws_nego_act.write(aws_nego_act_r, 4, action.T_a)
                    aws_nego_act.write(aws_nego_act_r, 5, action.B)
                    aws_nego_act.write(aws_nego_act_r, 6, action.T_b)
                    aws_nego_act.write(aws_nego_act_r, 7, action.Ox)
                    aws_nego_act.write(aws_nego_act_r, 8, action.x)
                    aws_nego_act.write(aws_nego_act_r, 9, "")

                    aws_nego_act_r += 1
            # END

            awb.close()
            del awb

        with open(join(world_folder, '.parsed'), 'w') as parsed:
            pass

        del data_dict


if __name__ == '__main__':
    run()
