import subprocess
from csv import reader
from datetime import datetime
import os
from subprocess import TimeoutExpired
import sys
from pathlib import Path
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
# ./eecbs-DaT-noW.exe --map ./instances/empty-16-16/empty-16-16.map --agents ./instances/empty-16-16/empty-16-16-random-40-agents-15.scen --output empty-16-16-DaT-noW-sub-1.csv --outputPaths empty-16-16-random-40-agents-15-paths.txt --agentNum 40 --cutoffTime 60 --suboptimality=1

# ./cbsh2-rtc.exe -m random-32-32-20.map -a random-32-32-20-random-1.scen -o test.csv --outputPaths=paths.txt -k 30 -t 60

def run(input_path, map_name, mapf_setting, instance_name, output_file_name, n_agents, time_limit, solver_name):
    
    print(solver_name, "starts to solve", instance_name, datetime.now().strftime("%H:%M:%S"))
    
    if solver_name.startswith('eecbs'):
        suboptimality = solver_name.split("-")[1]
        solver_name_ = solver_name.replace('-'+suboptimality, "")
    else:
        solver_name_ = solver_name


    args = [f"./solvers/{solver_name_}.exe",
         "--map",           input_path + map_name       + ".map",
         "--agents",        input_path + instance_name  + ".scen",
         "--output",        output_file_name + ".csv",
         "--outputPaths",   f"./{map_name} results/{solver_name}_solutions/{instance_name}-paths.txt",
         "--agentNum",      str(n_agents),
         "--cutoffTime",    str(time_limit),
        ]
    
    if solver_name.startswith('eecbs'):
        args += ["--suboptimality", suboptimality]
    
    process = subprocess.Popen(
        args,
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
    
    solvers = ['eecbs-1', 'eecbs-1.1', "cbsh2-rtc"]
    mapf_settings = ["noDaT-noW", "noDaT-wW", "DaT-noW", "DaT-wW"]
    n_agents_lst = [20,40,60,80]
    map_names = ['empty-16-16', 'empty-32-32', 'random-32-32-10', 'random-32-32-20']
    
    for map_name in map_names:

        for solver_name in solvers:
            
            for mapf_setting in mapf_settings:

                solver_name_ = solver_name + "-" + mapf_setting

                output_file_name = f"./result_tables/{map_name}-{solver_name_}"

                # checking if scenario was run before
                check_lst = []
                if os.path.isfile(f"{output_file_name}.csv"):
                   
                    with open(f"{output_file_name}.csv", 'r') as __file:
                        csv_reader = reader(__file)
                        for row in csv_reader:
                            check_lst.append(row[-1][:-1])

                p = Path(f"./solutions/{map_name}/{solver_name_}/")
                p.mkdir(parents=True, exist_ok=True)
                
                for n_agents in n_agents_lst:
                    
                    instance_idx_lst = range(1, 101) if map_name == 'empty-16-16' else range(1,26)

                    for instance_no in instance_idx_lst:

                        input_path = f"instances/{map_name}/"
                        instance_name = f"{map_name}-random-{n_agents}-agents-{instance_no}"

                        if os.path.isfile(f"./solutions/{map_name}/{solver_name_}/{instance_name}-paths.txt"):
                            continue
                        
                        if f"{input_path}{instance_name}.scen" in check_lst:
                            continue
        
                        run(input_path, map_name, mapf_setting, instance_name, output_file_name, n_agents, time_limit, solver_name_)
