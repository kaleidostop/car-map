package kaleidostop.map.car_map.modules.ride.controller;

import jakarta.validation.Valid;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.dto.CreateRideRequest;
import kaleidostop.map.car_map.modules.ride.dto.JoinRideRequest;
import kaleidostop.map.car_map.modules.ride.dto.RideRequestDto;
import kaleidostop.map.car_map.modules.ride.dto.RideResponse;
import kaleidostop.map.car_map.modules.ride.service.JoinRideService;
import kaleidostop.map.car_map.modules.ride.service.RideService;
import kaleidostop.map.car_map.modules.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rides")
public class RideController {
    private final RideService rideService;
    private final JoinRideService joinRideService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<?> createRide(@RequestBody @Valid CreateRideRequest request,
                                       Authentication auth) {
        User driver = (User) auth.getPrincipal();
        Ride ride = rideService.createRide(driver, request);
        return ResponseEntity.ok(ride);
    }

    @PostMapping("/{rideId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> joinRide(@PathVariable(name = "rideId")  Long rideId,
                                    @Valid @RequestBody JoinRideRequest request,
                                    Authentication auth) {
        User passenger = (User) auth.getPrincipal();
        Map<String, Object> result = joinRideService.joinRide(rideId, passenger, request);
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
        return ResponseEntity.ok(joinRideService.getPendingRequests(rideId, (User) auth.getPrincipal()));
    }

    @PatchMapping("/{rideId}/requests/{requestId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<?> handleRequest(@PathVariable(name = "rideId") Long rideId,
                                        @PathVariable(name = "requestId") Long requestId,
                                        @RequestParam(name = "action") String action,
                                        Authentication auth) {
        return ResponseEntity.ok(joinRideService.handleRequest(rideId, requestId, action, (User) auth.getPrincipal()));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RideResponse>> getActiveRides(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(rideService.getRidesByDriver(user));
    }

    @PatchMapping("/{rideId}/cancel")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<?> cancelRide(@PathVariable(name = "rideId") Long rideId, Authentication auth) {
        User driver = (User) auth.getPrincipal();
        rideService.cancelRide(rideId, driver);
        return ResponseEntity.ok(Map.of("message", "Поездка отменена"));
    }

    @GetMapping("/my/pending-requests-count")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<Long> getPendingRequestsCount(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(rideService.getPendingRequestsCount(user));
    }
}