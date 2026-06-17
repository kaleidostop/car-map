package kaleidostop.map.car_map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideRequest;
import kaleidostop.map.car_map.domain.RideRequestStatus;
import kaleidostop.map.car_map.domain.User;

public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    boolean existsByRideAndPassenger(Ride ride, User passenger);

    boolean existsByRideAndPassengerAndStatus(Ride ride, User passenger, RideRequestStatus status);

    boolean existsByRideAndPassengerAndStatusIn(Ride ride, User passenger, List<RideRequestStatus> statuses);

    @Query("SELECT r FROM RideRequest r WHERE r.ride.id = :rideId AND r.status = 'PENDING'")
    List<RideRequest> findPendingRequestsByRideId(@Param("rideId") Long rideId);

    long countByRideIdAndStatus(Long rideId, RideRequestStatus status);

    List<RideRequest> findByPassengerOrderByCreatedAtDesc(User passenger);
}