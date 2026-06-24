package kaleidostop.map.car_map.modules.routing.service;

import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import kaleidostop.map.car_map.modules.routing.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Route createRoute(RouteInfo routeInfo) {
        Route route = new Route();
        route.setDistanceMeters(routeInfo.getDistanceMeters());
        route.setDurationSeconds(routeInfo.getDurationSeconds());
        try {
            route.setPolyline(objectMapper.writeValueAsString(routeInfo.getGeometry()));
        } catch (Exception e) {
            log.warn("Failed to serialize route geometry", e);
        }
        return routeRepository.save(route);
    }

    public Route getRouteById(Long id) {
        return routeRepository.findById(id).orElse(null);
    }
}