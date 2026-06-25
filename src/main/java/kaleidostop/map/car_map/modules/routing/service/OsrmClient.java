package kaleidostop.map.car_map.modules.routing.service;

import kaleidostop.map.car_map.modules.routing.dto.OsrmResponse;
import kaleidostop.map.car_map.modules.routing.dto.OsrmRoute;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class OsrmClient {
    private final WebClient.Builder webClientBuilder;

    @Cacheable(value = "osrmRoutes", key = "#coordinates")
    public RouteInfo fetchRoute(String coordinates) {
        try {
            log.info("OSRM key: {}", coordinates);

            WebClient client = webClientBuilder.baseUrl("https://router.project-osrm.org").build();
            OsrmResponse response = client.get()
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
            return toRouteInfo(response.getRoutes().get(0));
        } catch (Exception e) {
            log.warn("Failed to fetch route from OSRM", e);
            return null;
        }
    }

    private RouteInfo toRouteInfo(OsrmRoute route) {
        RouteInfo info = new RouteInfo();
        info.setDistanceMeters(route.getDistance());
        info.setDurationSeconds(route.getDuration());
        info.setGeometry(route.getGeometry());
        return info;
    }
}