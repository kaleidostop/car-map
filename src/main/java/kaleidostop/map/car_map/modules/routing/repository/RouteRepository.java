package kaleidostop.map.car_map.modules.routing.repository;

import kaleidostop.map.car_map.modules.routing.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
}