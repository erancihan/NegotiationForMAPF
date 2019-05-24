package edu.ozu.drone;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.beans.ConstructorProperties;
import java.util.HashMap;

@RestController
public class RoutesRestController {
    HashMap<String, AgentData> agents = new HashMap<>();

    @PostMapping("/set-agent")
    @ResponseStatus(HttpStatus.OK)
    public void setAgent(@RequestBody AgentData agent)
    {
        agents.put(agent.id, agent);
        System.out.println("> set-agent " + agent.id);
    }

    @GetMapping(value = "/agent/{aid}", produces = "application/json")
    public HashMap<String, String> join(@PathVariable String aid)
    {
        AgentData agent = agents.get(aid);

        HashMap<String, String> resp = new HashMap<>();
        resp.put("id", agent.id);
        resp.put("x", agent.x);
        resp.put("y", agent.y);

        return resp;
    }
}

class AgentData {
    public String id;
    public String x;
    public String y;

    @ConstructorProperties({"id", "x", "y"})
    AgentData(String id, String x, String y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}