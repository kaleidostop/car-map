package kaleidostop.map.car_map.modules.routing.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.repository.RouteRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class RouteService {
    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private final RouteRepository routeRepository;
    private final ObjectMapper objectMapper;

    public RouteService(RouteRepository routeRepository, ObjectMapper objectMapper) {
        this.routeRepository = routeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Route createRoute(Map<String, Object> geometry, double distanceMeters, double durationSeconds) {
        Route route = new Route();
        route.setDistanceMeters(distanceMeters);
        route.setDurationSeconds(durationSeconds);
        try {
            route.setPolyline(objectMapper.writeValueAsString(geometry));
        } catch (Exception e) {
            log.warn("Failed to serialize route geometry", e);
        }
        return routeRepository.save(route);
    }

    public Route getRouteById(Long id) {
        return routeRepository.findById(id).orElse(null);
    }
}