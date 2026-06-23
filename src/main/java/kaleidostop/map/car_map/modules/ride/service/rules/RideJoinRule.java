package kaleidostop.map.car_map.modules.ride.service.rules;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;

public interface RideJoinRule {
    void check(Ride ride, RouteInfo currentRoute, RouteInfo newRoute);
}
