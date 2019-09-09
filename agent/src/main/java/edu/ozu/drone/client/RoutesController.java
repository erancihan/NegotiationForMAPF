package edu.ozu.drone.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling web ui paths
 * */
@Controller
public class RoutesController {
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

    @GetMapping("/watch")
    public String watch()
    {
        return "index";
    }
}
