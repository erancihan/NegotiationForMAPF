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
        agents.put(agent.aid, agent);
        System.out.println("> set-agent " + agent.aid);
    }

    @PostMapping(value = "/agent")
    public HashMap<String, String> join(@RequestBody AidWid target)
    {
        System.out.println(":" + target.aid);
        AgentData agent = agents.get(target.aid);
        agent.wid = target.wid;

        HashMap<String, String> resp = new HashMap<>();
        resp.put("wid", agent.wid);
        resp.put("aid", agent.aid);
        resp.put("x", agent.x);
        resp.put("y", agent.y);

        return resp;
    }
}

class AidWid
{
    public String aid;
    public String wid;

    @ConstructorProperties({"aid", "wid"})
    AidWid(String aid, String wid)
    {
        this.aid = aid;
        this.wid = wid;
    }
}

class AgentData {
    public String aid;
    public String wid;
    public String x;
    public String y;

    @ConstructorProperties({"id", "x", "y"})
    AgentData(String id, String x, String y)
    {
        this.aid = id;
        this.x = x;
        this.y = y;
    }
}