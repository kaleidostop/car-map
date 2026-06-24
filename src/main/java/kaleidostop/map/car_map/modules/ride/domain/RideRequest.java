package kaleidostop.map.car_map.modules.ride.domain;

import jakarta.persistence.*;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.ride.dto.JoinRideRequest;
import kaleidostop.map.car_map.modules.user.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ride_requests")
public class RideRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private User passenger;

    @Enumerated(EnumType.STRING)
    private RideRequestStatus status;

    private Double passengerDepartureLat;
    private Double passengerDepartureLon;
    private String passengerDepartureAddress;
    private LocalDateTime createdAt = LocalDateTime.now();

    public static RideRequest createPending(Ride ride, User passenger, JoinRideRequest request) {
        RideRequest req = new RideRequest();
        req.setRide(ride);
        req.setPassenger(passenger);
        req.setPassengerDepartureLat(request.getPassengerLat());
        req.setPassengerDepartureLon(request.getPassengerLon());
        req.setStatus(RideRequestStatus.PENDING);
        return req;
    }
}