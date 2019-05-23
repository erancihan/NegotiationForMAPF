package edu.ozu.drone;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoutesController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
