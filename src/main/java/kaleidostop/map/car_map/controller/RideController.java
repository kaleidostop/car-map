package kaleidostop.map.car_map.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.dto.CreateRideRequest;
import kaleidostop.map.car_map.dto.RideResponse;
import kaleidostop.map.car_map.service.RideService;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DRIVER', 'ADMIN')")
    public ResponseEntity<?> createRide(@RequestBody @Valid CreateRideRequest request,
                                       Authentication auth) {
        User driver = (User) auth.getPrincipal();
        Ride ride = rideService.createRide(
            driver,
            request.getOfficeId(),
            request.getDepartureAddress(),
            request.getDepartureLat(),
            request.getDepartureLon(),
            request.getDepartureTime(),
            request.getSeatsTotal()
        );
        return ResponseEntity.ok(ride);
    }

    @GetMapping
    public ResponseEntity<List<RideResponse>> getActiveRides(
            @RequestParam(name = "officeId", required = false) Long officeId) {
        return ResponseEntity.ok(rideService.getActiveRides(officeId));
    }
}