package kaleidostop.map.car_map.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/login")
    public String loginPage() {
        return "login";  
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; 
    }

    @GetMapping("/map")
    public String mapPage() {
        return "map"; 
    }

    @GetMapping("/create-ride")
    public String createRidePage() {
        return "create-ride";
    }

    @GetMapping("/my-rides")
    public String myRides() {
        return "my-rides";
    }

    @GetMapping("/my-requests")
    public String myRequestsPage() {
        return "my-requests";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
}