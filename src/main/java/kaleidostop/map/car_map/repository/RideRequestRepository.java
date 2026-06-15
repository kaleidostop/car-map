package kaleidostop.map.car_map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideRequest;
import kaleidostop.map.car_map.domain.RideRequestStatus;
import kaleidostop.map.car_map.domain.User;

public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    boolean existsByRideAndPassenger(Ride ride, User passenger);

    boolean existsByRideAndPassengerAndStatus(Ride ride, User passenger, RideRequestStatus status);

    boolean existsByRideAndPassengerAndStatusIn(Ride ride, User passenger, List<RideRequestStatus> statuses);
}