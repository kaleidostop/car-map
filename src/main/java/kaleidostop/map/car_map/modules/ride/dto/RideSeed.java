package kaleidostop.map.car_map.modules.ride.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideSeed {
    private String driverEmail;
    private String officeName;
    private String departureAddress;
    private double departureLat;
    private double departureLon;
    private int departureTimeOffset;
    private int seatsTotal;
    private String status;
}