package kaleidostop.map.car_map.modules.ride.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRideRequest {
    @NotNull
    private Double passengerLat;
    @NotNull
    private Double passengerLon;
    private String passengerAddress; 
}