package kaleidostop.map.car_map.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.dto.CreateRideRequest;
import kaleidostop.map.car_map.dto.JoinRideRequest;
import kaleidostop.map.car_map.dto.RideRequestDto;
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

    @PostMapping("/{rideId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> joinRide(@PathVariable(name = "rideId")  Long rideId,
                                    @Valid @RequestBody JoinRideRequest request,
                                    Authentication auth) {
        User passenger = (User) auth.getPrincipal();
        Map<String, Object> result = rideService.joinRide(rideId, passenger,
            request.getPassengerLat(), request.getPassengerLon());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<RideResponse>> getActiveRides(
            @RequestParam(name = "officeId", required = false) Long officeId) {
        return ResponseEntity.ok(rideService.getActiveRides(officeId));
    }

    @GetMapping("/{rideId}/requests")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<List<RideRequestDto>> getRequests(@PathVariable(name = "rideId") Long rideId, Authentication auth) {
        return ResponseEntity.ok(rideService.getPendingRequests(rideId, (User) auth.getPrincipal()));
    }

    @PatchMapping("/{rideId}/requests/{requestId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<?> handleRequest(@PathVariable(name = "rideId") Long rideId,
                                        @PathVariable(name = "requestId") Long requestId,
                                        @RequestParam(name = "action") String action,
                                        Authentication auth) {
        return ResponseEntity.ok(rideService.handleRequest(rideId, requestId, action, (User) auth.getPrincipal()));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RideResponse>> getMyRides(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(rideService.getRidesByDriver(user));
    }
}