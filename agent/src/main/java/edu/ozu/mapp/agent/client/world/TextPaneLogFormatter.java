package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.system.DATA_LOG_DISPLAY;
import edu.ozu.mapp.system.WorldOverseer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class TextPaneLogFormatter
{
    public LogDisplayPane scenario_info_pane;
    public JTextPane negotiation_info_pane;

    public SimpleAttributeSet attributes;

    public TextPaneLogFormatter()
    {
        attributes = new SimpleAttributeSet();
    }

    public void format(DATA_LOG_DISPLAY data)
    {
        try {
            parse_scenario_pane_log(data.clone());
            negotiation_info_pane.setText(parse_negotiation_pane_log(data));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void parse_scenario_pane_log(DATA_LOG_DISPLAY data)
    {
        scenario_info_pane.SetData(data);
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
