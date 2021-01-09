package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.system.DATA_LOG_DISPLAY;
import edu.ozu.mapp.system.WorldOverseer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class TextPaneLogFormatter
{
    public JTextPane scenario_info_pane;
    public JTextPane negotiation_info_pane;

    public void format(DATA_LOG_DISPLAY data)
    {
        try {
            scenario_info_pane.setText(parse_scenario_pane_log(data.clone()));
            negotiation_info_pane.setText(parse_negotiation_pane_log(data));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @NotNull
    private String parse_scenario_pane_log(DATA_LOG_DISPLAY data)
    {
        StringBuilder out = new StringBuilder();

        data.world_data
            .keySet().stream().sorted()
            .forEach(key -> {
                out.append(String.format("%-11s : %s\n", key, data.world_data.get(key)));
            });
        out.append("-------------\n");
        data.agent_to_point
            .keySet().stream().sorted()
            .forEach(key -> {
                String[] _data = WorldOverseer.getInstance().GetAgentData(key);

                out.append(String.format("%s POS: %s TOKEN: %s REMAINING_PATH_LEN: %S\n", key, _data[0], _data[1], _data[2]));
            });
        out.append("-------------\n");
        for (Object[] item : data.world_log)
        {
            out.append(String.format("%-23s %s\n", item[1].toString(), item[0].toString()));
        }

        return out.toString();
    }

    @NotNull
    private String parse_negotiation_pane_log(DATA_LOG_DISPLAY data)
    {
        return
            data.negotiation_logs
                .keySet().stream()
                .map(key -> {
                    String[] key_pair = key.split("-", 2);

                    String[] key_data = new String[]{"", ""};
                    key_data[0] = key_pair[0];
                    if (key_pair.length == 2) key_data[1] = key_pair[1];

                    ArrayList<String> values = data.negotiation_logs.get(key);

                    return
                        // write session key
                        String.format("> %s %s \n", key_data[0].substring(0, 7), key_data[1]) +
                        // write contracts
                        String.join("\n", values) +
                        "\n";
                })
                .collect(Collectors.joining())
            ;
    }
}
