package kaleidostop.map.car_map.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

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

    @GetMapping("/admin/offices")
    public String adminOfficesPage() {
        return "admin-offices";
    }
}