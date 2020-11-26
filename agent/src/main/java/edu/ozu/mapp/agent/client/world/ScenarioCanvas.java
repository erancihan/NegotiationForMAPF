package edu.ozu.mapp.agent.client.world;

import edu.ozu.mapp.utils.Globals;
import edu.ozu.mapp.utils.JSONAgentData;
import edu.ozu.mapp.utils.JSONWorldData;
import edu.ozu.mapp.utils.Point;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.awt.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ScenarioCanvas extends Canvas
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScenarioCanvas.class);

    private Jedis jedis;
    private JSONWorldData world;

    // { 'AGENT_ID': [ 'X-Y' , ... ] }
    private HashMap<String, ArrayList<Point>> history = new HashMap<>();
    private HashMap<String, JSONAgentData> agents_data = new HashMap<>();
    private HashMap<String, Point[]> agents = new HashMap<>();
    private HashMap<String, String> agent_colors = new HashMap<>();

    private HashSet<String> used_hex_colors = new HashSet<>();

    private Random rand = new Random();
    private int cell_size = 0;

    public ScenarioCanvas()
    {
        super();

        setBackground(new java.awt.Color(255, 255, 255, 255));
        setMinimumSize(new java.awt.Dimension(100, 200));
        setPreferredSize(new java.awt.Dimension(100, 200));
    }

    public void SetWorldData(JSONWorldData world_data)
    {
        this.world = world_data;
        logger.debug("world.world_id = " + world.world_id);
    }

    public void SetAgentsData(ArrayList<JSONAgentData> agents_data)
    {
        for (JSONAgentData agent_data : agents_data)
        {
            String agent_key = agent_data.agent_name.replace("agent:", "");

            this.agents_data.put(agent_key, agent_data);

            String agent_hex_color;
            do {
                agent_hex_color = rgb2hex(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            } while (agent_hex_color.isEmpty() && used_hex_colors.contains(agent_hex_color));
            used_hex_colors.add(agent_hex_color);

            this.agent_colors.put(agent_key, agent_hex_color);
        }
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        for (String agent_key : agents.keySet())
        {
            g.setColor(hex2rgb(agent_colors.get(agent_key)));   // handle color

            // DRAW AGENT TARGET LOCATION
            g.fillRect(
                (agents_data.get(agent_key).dest.x * cell_size),
                (agents_data.get(agent_key).dest.y * cell_size),
                cell_size, cell_size
            );
        }

        for (String agent_key : agents.keySet())
        {
            g.setColor(hex2rgb(agent_colors.get(agent_key)));   // handle color
            Point[] broadcast = agents.get(agent_key);

            // DRAW AGENT CURRENT LOCATION
            if (broadcast.length > 0)
            {
                Point agent_current_loc = broadcast[0];

                g.fillOval(
                        (agent_current_loc.x * cell_size),
                        (agent_current_loc.y * cell_size),
                        cell_size, cell_size
                );
            }

            // UPDATE AGENT HISTORY
            ArrayList<Point> path = history.get(agent_key);
            if (path.size() == 0 || !broadcast[0].equals(path.get(path.size()-1)))
            {   // current point does not exist in history
                // add it to history
                history.get(agent_key).add(broadcast[0]);
            }

            // DRAW AGENT PATH HISTORY
            for (int i = 0; i < path.size(); i++)
            {   // draw the path taken up until this point
                Point from = path.get(i);
                Point dest = i + 1 >= path.size() ? broadcast[0] : path.get(i + 1);

                g.drawLine(
                    (from.x * cell_size) + (cell_size / 2), (from.y * cell_size) + (cell_size / 2),
                    (dest.x * cell_size) + (cell_size / 2), (dest.y * cell_size) + (cell_size / 2)
                );
            }

            // DRAW AGENT BROADCAST
            for (int i = 0; i + 1 < broadcast.length; i++)
            {
                Point from = broadcast[i];
                Point dest = broadcast[i + 1];

                g.drawLine(
                    (from.x * cell_size) + (cell_size / 2), (from.y * cell_size) + (cell_size / 2),
                    (dest.x * cell_size) + (cell_size / 2), (dest.y * cell_size) + (cell_size / 2)
                );
            }
        }
    }

    public void Init()
    {
        Connect();

        // CALCULATE CELL SIZE
        cell_size = calculate_cell_size();

        CompletableFuture
            .runAsync(() -> {
                try {
                    do {
                        TimeUnit.MILLISECONDS.sleep(100);

                        Update();
                    } while (agents.size() != world.agent_count);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            })
        ;
    }

    public void Connect()
    {
        if (jedis == null)
        {
            jedis = new Jedis(Globals.REDIS_HOST, Globals.REDIS_PORT);
        }
    }

    public void Update()
    {
        if (jedis != null)
        {
            String WorldPath = String.format("world:%s:path", world.world_id);

            ScanParams params = new ScanParams().match("*");
            String cursor = ScanParams.SCAN_POINTER_START;

            do {
                ScanResult<Map.Entry<String, String>> results = jedis.hscan(WorldPath, cursor, params);

                results.getResult().forEach((result) -> {
                    String agent_name = result.getKey().replace("agent:", "");
                    Point[] agent_broadcast = String2BroadcastArray(result.getValue());

                    agents.put(agent_name, agent_broadcast);
                    history.put(agent_name, new ArrayList<>());
                });

                cursor = results.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
        }
    }

    public void Resize()
    {
        if (world != null)
        {
            cell_size = calculate_cell_size();
        }
    }

    public void Destroy()
    {
        if (jedis != null)
        {
            jedis.close();
        }
    }

    private Color hex2rgb(String hex)
    {
        return new Color(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }

    private String rgb2hex(int r, int g, int b)
    {
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private int calculate_cell_size()
    {
        int canvas_height = this.getHeight();
        int canvas_width  = this.getWidth();

        int shortest_corner = Math.min(canvas_height, canvas_width);
        int divider = Math.max(world.height, world.width);

        return shortest_corner / divider;
    }

    private Point[] String2BroadcastArray(String str)
    {
        return Arrays
                .stream(
                    str
                        .replaceAll("([\\[\\]]*)", "")
                        .split(",")
                )
                .map(p -> new Point(p, "-"))
                .toArray(Point[]::new)
        ;
    }
}
