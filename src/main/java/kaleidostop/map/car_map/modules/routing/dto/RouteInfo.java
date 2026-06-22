package kaleidostop.map.car_map.modules.routing.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteInfo {
    private double distanceMeters;
    private double durationSeconds;
    private Map<String, Object> geometry; 
}
