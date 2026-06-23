package kaleidostop.map.car_map.modules.routing.service;

import kaleidostop.map.car_map.modules.routing.dto.OsrmResponse;
import kaleidostop.map.car_map.modules.routing.dto.OsrmRoute;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class RoutingService {
    private final WebClient webClient;

    public RoutingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://router.project-osrm.org").build();
    }

    public RouteInfo getRoute(double fromLon, double fromLat, double toLon, double toLat) {
        return getRouteWithWaypoints(fromLon, fromLat, toLon, toLat, Collections.emptyList());
    }

    public RouteInfo getRouteWithWaypoints(double fromLon, double fromLat,
                                           double toLon, double toLat,
                                           List<double[]> waypoints) {
        StringBuilder coords = new StringBuilder();
        coords.append(fromLon).append(",").append(fromLat);
        for (double[] wp : waypoints) {
            coords.append(";").append(wp[0]).append(",").append(wp[1]);
        }
        coords.append(";").append(toLon).append(",").append(toLat);

        return fetchRoute(coords.toString());
    }

    private RouteInfo fetchRoute(String coordinates) {
        OsrmResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/v1/driving/{coordinates}")
                        .queryParam("overview", "full")
                        .queryParam("geometries", "geojson")
                        .build(coordinates))
                .retrieve()
                .bodyToMono(OsrmResponse.class)
                .onErrorResume(e -> Mono.empty())
                .block();

        if (response == null || response.getRoutes().isEmpty()) {
            return null;
        }

        OsrmRoute route = response.getRoutes().get(0);
        RouteInfo info = new RouteInfo();
        info.setDistanceMeters(route.getDistance());
        info.setDurationSeconds(route.getDuration());
        info.setGeometry(route.getGeometry());
        return info;
    }
}