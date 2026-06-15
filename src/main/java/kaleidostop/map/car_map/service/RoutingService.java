package kaleidostop.map.car_map.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import kaleidostop.map.car_map.dto.osrm.OsrmResponse;
import kaleidostop.map.car_map.dto.osrm.OsrmRoute;
import kaleidostop.map.car_map.dto.osrm.RouteInfo;
import reactor.core.publisher.Mono;

@Service
public class RoutingService {
    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    public RoutingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://router.project-osrm.org").build();
    }

    public RouteInfo getRoute(double fromLon, double fromLat, double toLon, double toLat) {
        String coordinates = fromLon + "," + fromLat + ";" + toLon + "," + toLat;
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

    public RouteInfo getRoute(double fromLon, double fromLat,
                            double waypointLon, double waypointLat,
                            double toLon, double toLat) {
        String coordinates = fromLon + "," + fromLat + ";" +
                                waypointLon + "," + waypointLat + ";" +
                                toLon + "," + toLat;
        return fetchRoute(coordinates);
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