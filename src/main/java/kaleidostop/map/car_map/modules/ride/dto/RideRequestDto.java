package kaleidostop.map.car_map.modules.ride.dto;

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
}