package kaleidostop.map.car_map.modules.routing.service;

import kaleidostop.map.car_map.modules.routing.dto.OsrmResponse;
import kaleidostop.map.car_map.modules.routing.dto.OsrmRoute;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OsrmClient {
    private final WebClient webClient;

    public OsrmClient(@Qualifier("osrmWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Cacheable(value = "osrmRoutes", key = "#coordinates")
    public RouteInfo fetchRoute(String coordinates) {
        try {
            log.info("OSRM key: {}", coordinates);

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
