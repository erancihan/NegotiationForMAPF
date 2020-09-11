import os
import json
from os.path import dirname, realpath, join, basename
from glob import glob

def parse_world_log(file_path: str):
    with open(file_path, 'r') as log:
        line: str
        for line in log:
            timestamp, entry = line.strip().split(';')
            data: dict = json.loads(entry)

            print(timestamp, data)

def run():
    for world_folder in glob(join(dirname(__file__), '..', 'logs', 'WORLD-*')):
        if os.path.exists(join(world_folder, '.parsed')):
            continue

        print(' ┬', world_folder)

        log_files = glob(join(world_folder, '*.log'))
        for i, log_file in enumerate(log_files):
            file_name = basename(log_file)

            if i < len(log_files)-1:
                print(' ├', file_name)
            else:
                print(' └', file_name)

            if 'WORLD' in file_name:
                parse_world_log(log_file)


if __name__ == '__main__':
    run()
