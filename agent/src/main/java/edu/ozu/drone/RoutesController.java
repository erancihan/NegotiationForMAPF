package edu.ozu.drone;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.beans.ConstructorProperties;

@Controller
public class RoutesController {
    AgentData agent;

    @PostMapping("/set-agent")
    @ResponseStatus(HttpStatus.OK)
    public void setAgent(@RequestBody AgentData agent) {
        System.out.println("> set-agent " + agent.id);
    }

    @GetMapping("/")
    public String index()
    {
        return "index";
    }

    @GetMapping("/login/{aid}")
    public String loginWithAgentID()
    {
        return "index";
    }

    @GetMapping("/worlds")
    public String worlds()
    {
        return "index";
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
