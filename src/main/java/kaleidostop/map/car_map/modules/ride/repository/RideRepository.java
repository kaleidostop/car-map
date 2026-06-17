package kaleidostop.map.car_map.modules.ride.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideStatus;
import kaleidostop.map.car_map.modules.user.domain.User;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByStatusAndOfficeId(RideStatus status, Long officeId);

    List<Ride> findByDriver(User driver);
}