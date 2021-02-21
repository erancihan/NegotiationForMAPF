import os
from os import path
from pathlib import Path
import json

OUT_FOR_XLSX = False

configs_folder = Path(path.join(path.dirname(__file__), "..", "artifacts", "configs"))

def a():
    __data = {}
    for file in configs_folder.glob("**/*.json"):
        __f0 = str(file.parents[0]).split('\\')[-1]
        __f1 = str(file.parents[1]).split('\\')[-1]
        __fname = file.name

        if __f1 not in __data:
            __data[__f1] = []

        __data[__f1].append(
            "{},{},{}".format(__fname.split('.')[0].split('_')[-1], __fname, __f0 == 'solved')
        )

    if OUT_FOR_XLSX:
        for key in __data:
            print(key)
            for __row in __data[key]:
                print(__row)
            print()


def b():
    __data = {}
    for file in configs_folder.glob("experiment_1/**/*.agents"):
        __fname = file.name

        conf_info = __fname.split('.')[0].split('-')

        x = conf_info[1]
        y = conf_info[2]

        idx = conf_info[-1]

        file_info = {"agents": [], "map": {"dimensions": []}}
        with open(file) as f:
            agent_c = f.readline().strip()

            agents = []
            while True:
                line = f.readline().strip()
                if not line:
                    break

                line_data = line.split(',')

                agent = {'start': [float(x) for x in line_data[:2]], 'goal': [float(x) for x in line_data[2:4]]}
                agents.append(agent)

            file_info['agents'] = agents
            file_info["map"]['dimensions'] = [int(x), int(y)]

        json_file = Path(configs_folder, "experiment_1", "configs", "{}x{}_{}_{}.json".format(x, y, agent_c, idx))

        with open(json_file, 'w') as outfile:
            json.dump(file_info, outfile)


if __name__ == '__main__':
    b()
