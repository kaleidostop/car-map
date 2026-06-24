package kaleidostop.map.car_map.modules.routing.dto;

import kaleidostop.map.car_map.modules.routing.domain.Route;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Getter
@Setter
public class RouteInfo {
    private double distanceMeters;
    private double durationSeconds;
    private Map<String, Object> geometry;

    public static RouteInfo from(Route route, ObjectMapper mapper) {
        if (route == null) return null;
        try {
            RouteInfo info = new RouteInfo();
            info.setDistanceMeters(route.getDistanceMeters());
            info.setDurationSeconds(route.getDurationSeconds());
            info.setGeometry(mapper.readValue(route.getPolyline(), Map.class));
            return info;
        } catch (Exception e) {
            return null;
        }
    }
}
