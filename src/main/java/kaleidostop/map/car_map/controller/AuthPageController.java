package kaleidostop.map.car_map.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {
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

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
}