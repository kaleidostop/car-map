package kaleidostop.map.car_map.modules.ride.dto;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RideRequestPassengerDto {
    private Long id;
    private Long rideId;
    private String departureAddress;
    private String officeName;
    private LocalDateTime departureTime;
    private RideRequestStatus status;
    private double passengerLat;
    private double passengerLon;
    private String driverName;
    private int seatsAvailable;

    public static RideRequestPassengerDto from(RideRequest r) {
        Ride ride = r.getRide();
        return new RideRequestPassengerDto(
                r.getId(),
                ride.getId(),
                ride.getDepartureAddress(),
                ride.getOffice().getName(),
                ride.getDepartureTime(),
                r.getStatus(),
                r.getPassengerDepartureLat() != null ? r.getPassengerDepartureLat() : 0.0,
                r.getPassengerDepartureLon() != null ? r.getPassengerDepartureLon() : 0.0,
                ride.getDriver().getFullName(),
                ride.getSeatsAvailable()
        );
    }
}