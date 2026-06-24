package kaleidostop.map.car_map.modules.ride.controller;

import kaleidostop.map.car_map.modules.ride.dto.RideRequestPassengerDto;
import kaleidostop.map.car_map.modules.ride.service.JoinRideService;
import kaleidostop.map.car_map.modules.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests")
public class RideRequestController {
    private final JoinRideService joinRideService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RideRequestPassengerDto>> getMyRequests(Authentication auth) {
        User passenger = (User) auth.getPrincipal();
        return ResponseEntity.ok(joinRideService.getRequestsForPassenger(passenger));
    }
}