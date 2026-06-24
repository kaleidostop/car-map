package kaleidostop.map.car_map.modules.ride.dto;

import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RideRequestDto {
    private Long id;
    private String passengerName;
    private double passengerLat;
    private double passengerLon;
    private double detourMeters;
    private double detourMinutes;

    public static RideRequestDto from(RideRequest req, double detourMeters, double detourMinutes) {
        return new RideRequestDto(
                req.getId(),
                req.getPassenger().getFullName(),
                req.getPassengerDepartureLat(),
                req.getPassengerDepartureLon(),
                detourMeters,
                detourMinutes
        );
    }
}