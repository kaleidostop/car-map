package kaleidostop.map.car_map.modules.ride.service.rules;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;

public class MaxDetourMetersRule implements RideJoinRule {
    @Override
    public void check(Ride ride, RouteInfo currentRoute, RouteInfo newRoute) {
        if (ride.getMaxDetourMeters() != null && ride.getMaxDetourMeters() > 0) {
            double detour = newRoute.getDistanceMeters() - currentRoute.getDistanceMeters();
            if (detour > ride.getMaxDetourMeters()) {
                throw new IllegalStateException("Превышено максимальное добавочное расстояние");
            }
        }
    }
}
