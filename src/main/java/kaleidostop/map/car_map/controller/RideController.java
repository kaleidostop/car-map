package kaleidostop.map.car_map.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideRequest;
import kaleidostop.map.car_map.domain.RideRequestStatus;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.dto.CreateRideRequest;
import kaleidostop.map.car_map.dto.JoinRideRequest;
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
    public ResponseEntity<?> joinRide(@PathVariable(name = "rideId")  Long rideId,
                                    @Valid @RequestBody JoinRideRequest request,
                                    Authentication auth) {
        User passenger = (User) auth.getPrincipal();
        try {
            RideRequest rideRequest = rideService.joinRide(rideId, passenger,
                    request.getPassengerLat(), request.getPassengerLon());
            return ResponseEntity.ok(Map.of(
                    "status", rideRequest.getStatus(),
                    "message", rideRequest.getStatus().equals(RideRequestStatus.ACCEPTED) ? "Вы присоединились к поездке" : "Запрос отклонён: превышено допустимое добавочное расстояние"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<RideResponse>> getActiveRides(
            @RequestParam(name = "officeId", required = false) Long officeId) {
        return ResponseEntity.ok(rideService.getActiveRides(officeId));
    }
}