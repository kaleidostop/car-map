package kaleidostop.map.car_map.modules.routing.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OsrmResponse {
    private List<OsrmRoute> routes;
}