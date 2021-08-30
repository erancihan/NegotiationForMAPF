import os
from pathlib import Path

import xlsxwriter

data = {
    "16x16": {
        "20": {},
        "40": {},
        "60": {},
        "80": {}
    }
}


# noinspection DuplicatedCode
def run(search_path, sheet_name='EECBS Result'):
    for result_file in Path(search_path).rglob('*.csv'):
        folder_name = os.path.dirname(result_file).split("\\")[-1]  # get file name only
        _, sub_optimality_x, act_type = folder_name.split("-")
        sub_optimality  = float(str(sub_optimality_x).split("_")[1])
        has_wait_action = act_type == "wW"

        print(">:", result_file, folder_name)
        with open(result_file) as _csv:
            for _csv_i, _csv_line in enumerate(_csv):
                csv_line = _csv_line.strip().split(",")

                if _csv_i == 0:
                    print(csv_line)
                    continue

                _, dim_x, dim_y, _, agent_c, _, instance_id = str(csv_line[-1].split("/")[-1]).replace(".scen", "").split("-")
                data["{}x{}".format(dim_x, dim_y)]["{}".format(agent_c)]["{}".format(instance_id)] = {
                    "agents": [],
                    # todo ... fetch data
                    #  ??
                }

                # print(_csv_i, agent_c, instance_id, csv_line)

        for txt_file in Path(os.path.dirname(result_file)).rglob("*.txt"):
            print(">|", txt_file)

            _, dim_x, dim_y, _, agent_c, _, instance_id, _ = str(txt_file).strip().split("\\")[-1].replace(".txt", "").split("-")
            # print(dim_x, dim_y, agent_c, instance_id)

            dim = int(dim_x)

            workbook_data = {}
            with open(txt_file) as _txt:
                for _txt_i, _txt_line in enumerate(_txt):
                    agent_id, agent_path = _txt_line.strip().split(":")
                    agent_path = str(agent_path).strip().split("->")[:-1]
                    agent_path = ["{}-{}".format(int(int(_pair) % dim), int(_pair) // dim) for _pair in agent_path]
                    taken_path_len = len(agent_path)

                    start_x = int(int(  agent_path[0].split("-")[0]) %  dim)
                    start_y = int(      agent_path[0].split("-")[1]) // dim

                    dest_x = int(int(   agent_path[-1].split("-")[0]) %  dim)
                    dest_y = int(       agent_path[-1].split("-")[1]) // dim

                    planned_path_len = abs(start_x - dest_x) + abs(start_y - dest_y)

                    path_len_diff: int = 0
                    if taken_path_len > 0:
                        path_len_diff = taken_path_len - planned_path_len

                    # print(agent_id, len(agent_path), agent_path)
                    workbook_data[agent_id] = {
                        "start": "{}-{}".format(start_x, start_y),
                        "dest": "{}-{}".format(dest_x, dest_y),
                        "final_path": str(agent_path),
                        "planned_path_len": planned_path_len,
                        "taken_path_len": taken_path_len,
                        "path_diff": path_len_diff
                    }

            workbook = xlsxwriter.Workbook(
                os.path.join(
                    os.path.dirname(txt_file),
                    os.path.basename(txt_file).split('.')[0] + '.xlsx'
                )
            )

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

            agent_names = sorted(list(workbook_data.keys()), key=lambda x: int(x.replace('Agent ', '')))

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
    run("C:\\Users\\cihan\\Documents\\MAPP\\logs\\mapp_eecbs\\")
