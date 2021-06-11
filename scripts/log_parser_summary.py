import os
from pathlib import Path

import numpy as np
import pandas
import xlsxwriter
from pandas import DataFrame

cbs_data = {}
workbook_data = {}


# noinspection DuplicatedCode
def work_cbs_data(xlsx_path, sheet_name):
    _, x, y, _, agent_c, _, config_id = str(os.path.basename(xlsx_path)).split('.')[0].split('-')

    wb_data: DataFrame = pandas.read_excel(xlsx_path, sheet_name=sheet_name)

    planned_path_col = wb_data['planned path len'].to_numpy(dtype=int)
    min_path_before = np.min(planned_path_col)
    avg_path_before = np.average(planned_path_col)
    max_path_before = np.max(planned_path_col)
    std_path_before = np.std(planned_path_col, ddof=1)

    taken_path_col = wb_data['taken path len'].to_numpy(dtype=int)
    min_path_after = np.min(taken_path_col)
    avg_path_after = np.average(taken_path_col)
    max_path_after = np.max(taken_path_col)
    std_path_after = np.std(taken_path_col, ddof=1)

    path_diff_col = wb_data['path diff'].to_numpy(dtype=int)
    min_path_diff = np.min(path_diff_col)
    avg_path_diff = np.average(path_diff_col)
    max_path_diff = np.max(path_diff_col)
    std_path_diff = np.std(path_diff_col, ddof=1)

    if "{}x{}_{}".format(x, y, agent_c) not in workbook_data:
        workbook_data["{}x{}_{}".format(x, y, agent_c)] = {}
    if config_id not in workbook_data["{}x{}_{}".format(x, y, agent_c)]:
        workbook_data["{}x{}_{}".format(x, y, agent_c)][config_id] = {}

    _data = {
        "result": False,

        "min_path_before": 0,
        "avg_path_before": 0,
        "max_path_before": 0,
        "std_path_before": 0,

        "min_path_after": 0,
        "avg_path_after": 0,
        "max_path_after": 0,
        "std_path_after": 0,

        "min_path_diff": 0,
        "avg_path_diff": 0,
        "max_path_diff": 0,
        "std_path_diff": 0,
    }
    if "solved" in str(xlsx_path).split("\\"):
        _data["result"] = True
        _data["min_path_before"] = min_path_before
        _data["avg_path_before"] = avg_path_before
        _data["max_path_before"] = max_path_before
        _data["std_path_before"] = std_path_before

        _data["min_path_after"] = min_path_after
        _data["avg_path_after"] = avg_path_after
        _data["max_path_after"] = max_path_after
        _data["std_path_after"] = std_path_after

        _data["min_path_diff"] = min_path_diff
        _data["avg_path_diff"] = avg_path_diff
        _data["max_path_diff"] = max_path_diff
        _data["std_path_diff"] = std_path_diff

    workbook_data["{}x{}_{}".format(x, y, agent_c)][config_id][sheet_name.replace('Result', '').strip()] = _data


# noinspection DuplicatedCode
def work_data(world_path):  # creates row data
    base_dir = str(world_path).replace(folder_location, '').split('\\')[0]
    if base_dir in ["completed_runs", "mapp_cbs"]:
        return
    if base_dir.startswith('__'):
        return

    path = str(world_path).split('\\')
    try:
        __split = path[-1].split('-')
        timestamp = ""

        if len(__split) == 5:
            _, timestamp, conf, _, result = path[-1].split('-')
            dims, agent_c, conf_id = conf.split('_')
            workbook_sheet_key = f"{dims}_{agent_c}"
        if len(__split) == 6:
            _, timestamp, conf, conf_id, _, result = path[-1].split('-')
            dims, agent_c = conf.split('_')
            workbook_sheet_key = f"{dims}_{agent_c}"
        if timestamp == "":
            exit(500)
    except Exception:
        print()
        print(path[-1].split('-'))

        exit()

    agent_type = "Hybrid" if "Hybrid" in path[-2].split('_') else "Random"
    agent_fov = "FoV5" if "FoV" not in path[-2] else "_".join(path[-2].split('_')[3:])

    min_path_before: int = None
    avg_path_before: float = None
    max_path_before: int = None
    std_path_before: float = None

    min_path_after: int = None
    avg_path_after: float = None
    max_path_after: int = None
    std_path_after: float = None

    min_path_diff: int = None
    avg_path_diff: float = None
    max_path_diff: int = None
    std_path_diff: float = None

    min_num_nego: int = None
    avg_num_nego: float = None
    max_num_nego: int = None
    std_num_nego: float = None

    min_token_exchange: int = None
    avg_token_exchange: float = None
    max_token_exchange: int = None
    std_token_exchange: float = None

    min_retain_bid_diff: int = None
    avg_retain_bid_diff: float = None
    max_retain_bid_diff: int = None
    std_retain_bid_diff: float = None

    final_token_max: int = None
    final_token_min: int = None
    sum_of_num_of_tokens_received: int = 0

    if result == "true":
        wb_name = 'World-' + '-'.join(path[-1].split('-')[1:-1]) + '.xlsx'
        wb_data: DataFrame = pandas.read_excel(os.path.join(world_path, wb_name), sheet_name="Agents")

        planned_path_col = wb_data['planned path len'].to_numpy(dtype=int)
        min_path_before = np.min(planned_path_col)
        avg_path_before = np.average(planned_path_col)
        max_path_before = np.max(planned_path_col)
        std_path_before = np.std(planned_path_col, ddof=1)

        taken_path_col = wb_data['taken path len'].to_numpy(dtype=int)
        min_path_after = np.min(taken_path_col)
        avg_path_after = np.average(taken_path_col)
        max_path_after = np.max(taken_path_col)
        std_path_after = np.std(taken_path_col, ddof=1)

        path_diff_col = wb_data['path diff'].to_numpy(dtype=int)
        min_path_diff = np.min(path_diff_col)
        avg_path_diff = np.average(path_diff_col)
        max_path_diff = np.max(path_diff_col)
        std_path_diff = np.std(path_diff_col, ddof=1)

        negotiation_count_col = wb_data['negotiation count'].to_numpy(dtype=int)
        min_num_nego = np.min(negotiation_count_col)
        avg_num_nego = np.average(negotiation_count_col)
        max_num_nego = np.max(negotiation_count_col)
        std_num_nego = np.std(negotiation_count_col, ddof=1)

        token_exchange_col = wb_data['token_diff'].to_numpy(dtype=int)
        min_token_exchange = np.min(token_exchange_col)
        avg_token_exchange = np.average(token_exchange_col)
        max_token_exchange = np.max(token_exchange_col)
        std_token_exchange = np.std(token_exchange_col, ddof=1)

        retain_bid_diff_col = wb_data['retain_bid_diff'].to_numpy(dtype=int)
        min_retain_bid_diff = np.min(retain_bid_diff_col)
        avg_retain_bid_diff = np.average(retain_bid_diff_col)
        max_retain_bid_diff = np.max(retain_bid_diff_col)
        std_retain_bid_diff = np.std(retain_bid_diff_col, ddof=1)

        planned_path_len: int
        taken_path_len: int
        for idx in range(wb_data.shape[0]):
            if final_token_max is None or int(wb_data['final token count'][idx]) > final_token_max:
                final_token_max = int(wb_data['final token count'][idx])
            if final_token_min is None or int(wb_data['final token count'][idx]) < final_token_min:
                final_token_min = int(wb_data['final token count'][idx])

            sum_of_num_of_tokens_received += int(wb_data['# of tokens received'][idx])

    if workbook_sheet_key not in workbook_data:
        workbook_data[workbook_sheet_key] = {}
    if conf_id not in workbook_data[workbook_sheet_key]:
        workbook_data[workbook_sheet_key][conf_id] = {}

    workbook_data[workbook_sheet_key][conf_id]["{}_{}".format(agent_type, agent_fov)] = {
        "result": result == "true",
        "min_path_before": min_path_before,
        "avg_path_before": avg_path_before,
        "max_path_before": max_path_before,
        "std_path_before": std_path_before,

        "min_path_after": min_path_after,
        "avg_path_after": avg_path_after,
        "max_path_after": max_path_after,
        "std_path_after": std_path_after,

        "min_path_diff": min_path_diff,
        "avg_path_diff": avg_path_diff,
        "max_path_diff": max_path_diff,
        "std_path_diff": std_path_diff,

        "min_num_nego": min_num_nego,
        "avg_num_nego": avg_num_nego,
        "max_num_nego": max_num_nego,
        "std_num_nego": std_num_nego,

        "min_retain_bid_diff": min_retain_bid_diff,
        "avg_retain_bid_diff": avg_retain_bid_diff,
        "max_retain_bid_diff": max_retain_bid_diff,
        "std_retain_bid_diff": std_retain_bid_diff,

        "min_token_exchange": min_token_exchange,
        "avg_token_exchange": avg_token_exchange,
        "max_token_exchange": max_token_exchange,
        "std_token_exchange": std_token_exchange,

        "final_token_max": final_token_max,
        "final_token_min": final_token_min,

        "sum_of_num_of_tokens_received": sum_of_num_of_tokens_received,
    }


def run():
    _file_idx = 0
    for world_xlsx in Path(folder_location).rglob('WORLD-*-true'):
        _file_idx += 1
        print(f"\r{_file_idx}", end='')
        work_data(world_xlsx)
    print()

    for world_xlsx in Path(folder_location).rglob('WORLD-*-false'):
        _file_idx += 1
        print(f"\r{_file_idx}", end='')
        work_data(world_xlsx)
    print()

    _file_idx = 0
    for cbs_xlsx in Path(os.path.join(folder_location, "mapp_cbs")).rglob("*.xlsx"):
        _file_idx += 1
        print(f"\r{_file_idx}", end='')
        work_cbs_data(cbs_xlsx, 'CBS Result')
    print()

    _file_idx = 0
    for cbsh2_xlsx in Path(os.path.join(folder_location, "mapp_cbsh2")).rglob("*.xlsx"):
        _file_idx += 1
        print(f"\r{_file_idx}", end='')
        work_cbs_data(cbsh2_xlsx, 'CBSH2 Result')
    print()

    workbook = xlsxwriter.Workbook(os.path.join(folder_location, "RunResults.xlsx"))
    for sheet_key in ["8x8_10", "8x8_15", "8x8_20", "8x8_25", "16x16_20", "16x16_40", "16x16_60", "16x16_80"]:
        dim, agent_c = sheet_key.split("_")
        a, b = dim.split('x')

        sheet = workbook.add_worksheet(sheet_key)

        agent_types = list(workbook_data[sheet_key][str(1)].keys())
        print(agent_types)
        agent_types = sorted(agent_types)

        s_headers = [
            "min_path_before",
            "avg_path_before",
            "max_path_before",
            "std_path_before",

            "min_path_after",
            "avg_path_after",
            "max_path_after",
            "std_path_after",

            "min_path_diff",
            "avg_path_diff",
            "max_path_diff",
            "std_path_diff",

            "min_num_nego",
            "avg_num_nego",
            "max_num_nego",
            "std_num_nego",

            "min_retain_bid_diff",
            "avg_retain_bid_diff",
            "max_retain_bid_diff",
            "std_retain_bid_diff",

            "min_token_exchange",
            "avg_token_exchange",
            "max_token_exchange",
            "std_token_exchange",

            "final_token_max",
            "final_token_min",

            "sum_of_num_of_tokens_received",
        ]

        sheet_c = 0
        sheet_r = 0

        sheet.write(sheet_r, sheet_c, "Config ID")
        sheet_c += 1
        sheet.write(sheet_r, sheet_c, "Agent Type")
        sheet_c += 1
        sheet.write(sheet_r, sheet_c, "solved")
        sheet_c += 1
        for s_header in s_headers:
            sheet.write(sheet_r, sheet_c, s_header)
            sheet_c += 1
        sheet.write(sheet_r, sheet_c, 'cbs_solved')
        sheet_c += 1
        sheet.write(sheet_r, sheet_c, 'cbsh2_solved')
        sheet_c += 1

        sheet_r += 1

        config_id = 1
        for _ in range(len(workbook_data[sheet_key])):
            # BEGIN LOOP : CONFIG
            for agent_type in agent_types:
                # BEGIN LOOP : AGENT ONE-LINER
                sheet_c = 0  # move col cursor to start

                sheet.write_number(sheet_r, sheet_c, int(config_id))
                sheet_c += 1
                sheet.write(sheet_r, sheet_c, agent_type)
                sheet_c += 1

                if agent_type == 'CBS':
                    _exists = os.path.exists(
                        os.path.join(folder_location, 'mapp_cbs', 'solved', f"empty-{a}-{b}-random-{agent_c}-agents-{config_id}.json")
                    )
                    sheet.write(sheet_r, sheet_c, _exists)
                elif agent_type == 'CBSH2':
                    _exists = os.path.exists(
                        os.path.join(folder_location, 'mapp_cbsh2', 'solved', f"empty-{a}-{b}-random-{agent_c}-agents-{config_id}.json")
                    )
                    sheet.write(sheet_r, sheet_c, _exists)
                else:
                    if agent_type not in workbook_data[sheet_key][str(config_id)]:
                        continue
                    sheet.write(sheet_r, sheet_c, workbook_data[sheet_key][str(config_id)][agent_type]["result"])
                sheet_c += 1
                for s_header in s_headers:
                    sheet.write(sheet_r, sheet_c, workbook_data[sheet_key][str(config_id)][agent_type].get(s_header, ''))
                    sheet_c += 1
                # Has CBS Solved?
                _exists = os.path.exists(
                    os.path.join(folder_location, 'mapp_cbs', 'solved', f"empty-{a}-{b}-random-{agent_c}-agents-{config_id}.json")
                )
                sheet.write(sheet_r, sheet_c, _exists)
                sheet_c += 1
                # Has CBSH2 Solved
                _exists = os.path.exists(
                    os.path.join(folder_location, 'mapp_cbsh2', 'solved', f"empty-{a}-{b}-random-{agent_c}-agents-{config_id}.json")
                )

                sheet.write(sheet_r, sheet_c, _exists)
                sheet_c += 1

                sheet_r += 1
                # END LOOP : AGENT ONE-LINER
            # go to next config
            config_id += 1
            # END LOOP : CONFIG

    workbook.close()


folder_location: str

if __name__ == '__main__':
    folder_location = "C:\\Users\\cihan\\Documents\\MAPP\\logs\\"
    run()
