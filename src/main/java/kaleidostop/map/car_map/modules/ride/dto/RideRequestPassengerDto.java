package kaleidostop.map.car_map.modules.ride.dto;

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
}