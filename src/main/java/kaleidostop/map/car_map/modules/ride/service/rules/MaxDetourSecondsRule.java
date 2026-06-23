package kaleidostop.map.car_map.modules.ride.service.rules;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;

public class MaxDetourSecondsRule implements RideJoinRule {
    @Override
    public void check(Ride ride, RouteInfo currentRoute, RouteInfo newRoute) {
        if (ride.getMaxDetourSeconds() != null && ride.getMaxDetourSeconds() > 0) {
            double detourSeconds = newRoute.getDurationSeconds() - currentRoute.getDurationSeconds();
            if (detourSeconds > ride.getMaxDetourSeconds()) {
                throw new IllegalStateException("Превышено максимальное добавочное расстояние");
            }
        }
    }
}
