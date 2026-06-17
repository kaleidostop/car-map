package kaleidostop.map.car_map.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.dto.RideRequestPassengerDto;
import kaleidostop.map.car_map.service.RideService;

@RestController
@RequestMapping("/api/requests")
public class RideRequestController {
    private final RideService rideService;

    public RideRequestController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RideRequestPassengerDto>> getMyRequests(Authentication auth) {
        User passenger = (User) auth.getPrincipal();
        return ResponseEntity.ok(rideService.getRequestsForPassenger(passenger));
    }
}