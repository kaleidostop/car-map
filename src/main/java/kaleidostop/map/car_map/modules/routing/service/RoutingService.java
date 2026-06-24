package kaleidostop.map.car_map.modules.routing.service;

import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingService {
    private final OsrmClient osrmClient;

    public RouteInfo getRoute(double fromLon, double fromLat, double toLon, double toLat) {
        return getRouteWithWaypoints(fromLon, fromLat, toLon, toLat, Collections.emptyList());
    }

    public RouteInfo getRouteWithWaypoints(double fromLon, double fromLat, double toLon, double toLat, List<double[]> waypoints) {
        String coords = buildCoordinates(fromLon, fromLat, toLon, toLat, waypoints);
        return osrmClient.fetchRoute(coords);
    }

    private String buildCoordinates(double fromLon, double fromLat, double toLon, double toLat, List<double[]> waypoints) {
        StringBuilder sb = new StringBuilder();
        sb.append(fromLon).append(",").append(fromLat);
        for (double[] wp : waypoints) {
            sb.append(";").append(wp[0]).append(",").append(wp[1]);
        }
        sb.append(";").append(toLon).append(",").append(toLat);
        return sb.toString();
    }
}