package kaleidostop.map.car_map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideStatus;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByStatus(RideStatus status);
}