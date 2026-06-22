package kaleidostop.map.car_map.modules.ride.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideRequestDto {
    private Long id;
    private String passengerName;
    private double passengerLat;
    private double passengerLon;

    public RideRequestDto(Long id, String passengerName, double passengerLat, double passengerLon) {
        this.id = id;
        this.passengerName = passengerName;
        this.passengerLat = passengerLat;
        this.passengerLon = passengerLon;
    }
}