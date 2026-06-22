package kaleidostop.map.car_map.modules.routing.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OsrmRoute {
    private double distance;
    private double duration;
    private Map<String, Object> geometry;
}
