package edu.ozu.drone;

import edu.ozu.drone.utils.Point;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class AgentClient extends Runner {
    private String PORT = "";
    private String WS_URL = "ws://";

    protected Boolean IS_HEADLESS = false;

    protected String AGENT_NAME = "";
    protected String AGENT_ID   = "";
    protected Point START;
    protected Point DEST;

    private List<String> path;

    public AgentClient() { }

    public void init() { }

    void __init(String port) {
        this.PORT = port;
        init();
    }

    void run() {
        System.out.println(AGENT_NAME);

        path = calculatePath();
    }

    /**
     * The function that will be invoked by WebUI when player selects to join a world
     *
     * @param worldId id of the world the agent will join to
     * */
    void join(String worldId) {
        // todo
        // get path [0, 2]

        // join
        // post localhost:3001/join payload:{world_id, agent_id, agent_x, agent_y}

        // @response: watch()
    }

    /**
     * The function that be invoked at the end of each session
     * (world_state == 2)
     *
     * */
    private void move() {
        // post localhost:3001/move payload:{world_id, agent_id, agent_x, agent_y, direction}
        // direction -> {N, W, E, S}

        // response should match with next path point in line
    }

    /**
     * The function that is invoked after agent joins a world. Allows agent
     * to observe the state of the environment.
     *
     * Essentially handles invocations of other functions depending on the
     * {world_state}:
     * 0 -> collision check
     * 1 -> negotiation step
     * 2 -> move step
     *
     * */
    private void watch() {
        // ws://localhost:3001/world/{world_id}/{agent_id}

        // collision check @ every incoming message
        hasCollisions();

        // ping server
    }

    private boolean hasCollisions() {
        return false;
    }


    public List<String> calculatePath() {
        return AStar(START, DEST);
    }

    //<editor-fold desc="A-Star implementation">
    private List<String> AStar(Point start, Point goal) {
        int T = 0;
        double inf = Double.MAX_VALUE;

        HashMap<String, String> links = new HashMap<>();

        PriorityQueue<AStarNode> open = new PriorityQueue<>();
        List<Point> closed = new ArrayList<>();
        HashMap<String, Double> g = new HashMap<>();
        g.put(start.key, 0.0);

        open.add(new AStarNode(start, start.ManhattanDistTo(goal)));

        while (!open.isEmpty()) {
            AStarNode currentNode = open.remove();
            Point current = currentNode.point;

            if (closed.contains(current)) {
                continue;
            }

            closed.add(current);
            T = T + 1;

            if (current.equals(goal)) {
                return constructPath(links, start, goal);
            }

            List<Point> neighbours = getNeighbours(current, T);
            for (Point neighbour : neighbours) {
                if (closed.contains(neighbour)) {
                    continue;
                }

                // d <- g[current] + COST(current, neighbour, g)
                double d = g.getOrDefault(current.key, inf) + current.ManhattanDistTo(neighbour);

                if (d < g.getOrDefault(neighbour.key, inf)) {
                    g.put(neighbour.key, d);

                    links.put(neighbour.key, current.key);
                    d = d + neighbour.ManhattanDistTo(goal);
                    open.add(new AStarNode(neighbour, d));
                }
            }
        }
        return null;
    }

    // todo retrieve env dims
    private List<Point> getNeighbours(Point point, int t) {
        List<Point> nodes = new ArrayList<>();

        for (int i = 0; i < 9; i++) { // position of point
            if (i % 2 == 0) {
                continue;
            }

            int x = point.x + (i % 3) - 1;
            int y = point.y + (i / 3) - 1;

            if (x < 0 /*|| x >= env.width*/) { // x out of bounds
                continue;
            }
            if (y < 0 /*|| y >= env.height*/) { // y out of bounds
                continue;
            }

            Point n = new Point(x, y);
            nodes.add(n);
//            if (!env.isOccupiedAt(n.key, t)) { nodes.add(n); }
        }

        return nodes;
    }

    private List<String> constructPath(HashMap<String, String> links, Point start, Point goal) {
        List<String> path = new ArrayList<>();

        if (links.isEmpty()) {
            return path;
        }

        String next = goal.key;
        while (!next.equals(start.key)) {
            path.add(next);
            next = links.get(next);
        }
        path.add(start.key);

        Collections.reverse(path);

        return path;
    }

    private class AStarNode implements Comparable<AStarNode> {
        Point point;
        private double dist;

        AStarNode(Point point, double dist) {
            this.point = point;
            this.dist = dist;
        }

        @Override
        public int compareTo(AStarNode o) {
            return Double.compare(this.dist, o.dist);
        }

        @Override
        public String toString() {
            return String.format("%s:%.2f", point.key, dist);
        }
    }
    //</editor-fold>

    //<editor-fold desc="runner functions">
    @SuppressWarnings("Duplicates")
    void __setAgentAtController() {
        assert !PORT.isEmpty();
        assert !AGENT_ID.isEmpty();
        assert START != null;

        System.out.println("> passing agent data to backend controller");
        try {
            URL url = new URL("http://localhost:" + PORT + "/set-agent");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            String jsonInputString = "{" +
                    "\"id\": \"" + AGENT_ID + "\"," +
                    "\"x\": \""  + START.x  + "\"," +
                    "\"y\": \""  + START.y  + "\" }";

            try (OutputStream outs = con.getOutputStream())
            {
                byte[] inb = jsonInputString.getBytes(StandardCharsets.UTF_8);
                outs.write(inb, 0, inb.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
            {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null)
                {
                    response.append(responseLine);
                }
                System.out.println("> passed: " + response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void __launchBrowser() {
        if (IS_HEADLESS) { return; } // don't launch browser

        assert !AGENT_ID.isEmpty();

        String _os = System.getProperty("os.name").toLowerCase();
        String url = "http://localhost:" + PORT + "/login/" + AGENT_ID;

        System.out.println("> host os: " + _os);
        System.out.println("> routing: " + url);

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime runtime = Runtime.getRuntime();
                if (_os.contains("mac")) {
                    runtime.exec("open " + url);
                } else if (_os.contains("nix") || _os.contains("nux")) {
                    runtime.exec("xdg-open " + url);
                } else {
                    System.out.println("> BUMP!! UNHANDLED OS!!");
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
    //</editor-fold>
}
