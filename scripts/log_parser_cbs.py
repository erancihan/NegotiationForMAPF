import json
from glob import glob
import os
from pathlib import Path
import xlsxwriter


def run(folder_path, sheet_name):
    for result_file in Path(folder_path).rglob('*.json'):
        print(result_file)
        with open(result_file) as file:
            result_data = json.load(file)

            workbook_data = {}
            for agent in result_data['agents']:
                taken_path: str = ""
                taken_path_len: int = 0

                if 'plan' not in result_data:
                    result_data['plan'] = {}

                if agent['name'] in result_data['plan']:
                    taken_path += "["
                    _t = 0
                    for node in result_data['plan'][agent['name']]:
                        assert _t == int(node["t"])
                        taken_path += "{}-{}".format(node["x"], node["y"])
                        taken_path_len = _t
                        _t += 1
                    taken_path += "]"

                planned_path_len = \
                    abs(int(agent['start'][0]) - int(agent['goal'][0]))\
                    +\
                    abs(int(agent['start'][1]) - int(agent['goal'][1]))

                path_len_diff: int = 0
                if taken_path_len > 0:
                    path_len_diff = taken_path_len - planned_path_len

                workbook_data[agent['name']] = {
                    "start": "{}-{}".format(agent['start'][0], agent['start'][1]),
                    "dest": "{}-{}".format(agent['goal'][0], agent['goal'][1]),
                    "final_path": taken_path,
                    "planned_path_len": planned_path_len,
                    "taken_path_len": taken_path_len,
                    "path_diff": path_len_diff
                }

            workbook = xlsxwriter.Workbook(
                os.path.join(os.path.dirname(result_file), os.path.basename(result_file).split('.')[0] + '.xlsx'))
            sheet = workbook.add_worksheet(sheet_name)

            sheet_r = 0
            sheet.write(sheet_r, 0, "Agent ID")
            sheet.write(sheet_r, 1, "starting point")
            sheet.write(sheet_r, 2, "destination")
            sheet.write(sheet_r, 3, "final path")
            sheet.write(sheet_r, 4, "planned path len")
            sheet.write(sheet_r, 5, "taken path len")
            sheet.write(sheet_r, 6, "path diff")
            sheet_r += 1

            agent_names = sorted(list(workbook_data.keys()), key=lambda x: int(x.replace('agent', '')))

            for agent_name in agent_names:
                sheet.write(sheet_r, 0, agent_name)
                sheet.write(sheet_r, 1, workbook_data[agent_name]['start'])
                sheet.write(sheet_r, 2, workbook_data[agent_name]['dest'])
                sheet.write(sheet_r, 3, workbook_data[agent_name]['final_path'])
                sheet.write_number(sheet_r, 4, workbook_data[agent_name]['planned_path_len'])
                sheet.write_number(sheet_r, 5, workbook_data[agent_name]['taken_path_len'])
                sheet.write_number(sheet_r, 6, workbook_data[agent_name]['path_diff'])
                sheet_r += 1

            workbook.close()


if __name__ == '__main__':
    run("C:\\Users\\cihan\\Documents\\MAPP\\logs\\mapp_cbs", 'CBS Result')
    # run("C:\\Users\\cihan\\Documents\\MAPP\\logs\\mapp_cbsh2", 'CBSH2 Result')
