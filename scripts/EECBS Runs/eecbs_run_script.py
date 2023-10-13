import subprocess
from csv import reader
from datetime import datetime
import os
from subprocess import TimeoutExpired
import sys

"""
Runner script for scenarios
example args: -m random-32-32-20.map -a random-32-32-20-random-1.scen -o test.csv --outputPaths=paths.txt -k 50 -t 60 --suboptimality=1.2 
m: the map file from the MAPF benchmark
a: the scenario file from the MAPF benchmark
o: the output file that contains the search statistics
outputPaths: the output file that contains the paths
k: the number of agents
t: the runtime limit
suboptimality: the suboptimality factor w
"""

# example command for running eecbs exe file
# .\eecbs-DaT-noW.exe --map ./instances/empty-16-16/empty-16-16.map --agents ./instances/empty-16-16/empty-16-16-random-40-agents-15.scen --output empty-16-16-DaT-noW-sub-1.csv --outputPaths empty-16-16-random-40-agents-15-paths.txt --agentNum 40 --cutoffTime 60 --suboptimality=1

def run(input_path, map_name, instance_name, output_file_name, n_agents, eecbs_config, suboptimality):
    
    print(instance_name, "started at", datetime.now().strftime("%H:%M:%S"))
    
    process = subprocess.Popen(
        [f"eecbs_configs/eecbs-{eecbs_config}",
         "--map",           input_path + map_name       + ".map",
         "--agents",        input_path + instance_name  + ".scen",
         "--output",        output_file_name + ".csv",
         "--outputPaths",   instance_name + "-paths.txt",
         "--agentNum",      str(n_agents),
         "--cutoffTime",    str(time_limit), 
         "--suboptimality", str(suboptimality)
        ],
        stdout=subprocess.PIPE
    )

    outs, errs = None, None
    
    try:
        outs, errs = process.communicate(timeout=(time_limit + 60))
    except TimeoutExpired:
        print(instance_name, "killed at", datetime.now().strftime("%H:%M:%S"))
        process.kill()
        outs, errs = process.communicate()
    
    print(outs)
    
    if errs:
        print(errs)

if __name__ == '__main__':

    """
    MAPF Settings 1, 2, 3, and 4, respectively:

    "noDaT-noW" : No disappear-at-target & No wait action
    "noDaT-wW" : No disappear-at-target & With wait action
    "DaT-noW": Disappear-at-target & No wait action
    "DaT-wW": Disappear-at-target & With wait action
    """

    time_limit = 60 #secs
    
    eecbs_config_lst = ["noDaT-noW", "noDaT-wW", "DaT-noW", "DaT-wW"] 

    suboptimality_lst = [1, 1.1]

    n_agents_lst = [20, 40, 60, 80]


    for eecbs_config in eecbs_config_lst:
        for suboptimality in suboptimality_lst:

            output_file_name = f"empty-16-16-{eecbs_config}-sub-{suboptimality}"


            # checking if scenario was run before
            check_lst = []
            if os.path.isfile(f"{output_file_name}.csv"):
                with open(f"{output_file_name}.csv", 'r') as __file:
                    csv_reader = reader(__file)
                    for row in csv_reader:
                        check_lst.append(row[-1])


            for n_agents in n_agents_lst:
                
                for instance_no in range(1, 101):

                    input_path = "instances/empty-16-16/"
                    map_name = "empty-16-16"
                    instance_name = f"empty-16-16-random-{n_agents}-agents-{instance_no}"

                    if os.path.isfile(f"{instance_name}-paths.txt"):
                        continue
                    
                    if f"{input_path}{instance_name}.scen" in check_lst:
                        continue

                    run(input_path, map_name, instance_name, output_file_name, n_agents, time_limit, eecbs_config, suboptimality)
