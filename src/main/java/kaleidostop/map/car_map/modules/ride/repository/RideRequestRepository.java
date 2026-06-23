package kaleidostop.map.car_map.modules.ride.repository;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    boolean existsByRideAndPassenger(Ride ride, User passenger);

    boolean existsByRideAndPassengerAndStatus(Ride ride, User passenger, RideRequestStatus status);

    boolean existsByRideAndPassengerAndStatusIn(Ride ride, User passenger, List<RideRequestStatus> statuses);

    List<RideRequest> findByRideIdAndStatus(Long rideId, RideRequestStatus status);

    List<RideRequest> findByRideIdAndStatusIn(Long rideId, List<RideRequestStatus> statuses);

    long countByRideIdAndStatus(Long rideId, RideRequestStatus status);

    List<RideRequest> findByPassengerOrderByCreatedAtDesc(User passenger);
}