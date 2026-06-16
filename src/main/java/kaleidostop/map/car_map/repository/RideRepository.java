package kaleidostop.map.car_map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideStatus;
import kaleidostop.map.car_map.domain.User;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByStatusAndOfficeId(RideStatus status, Long officeId);

    List<Ride> findByDriver(User driver);
}