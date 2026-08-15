package kaleidostop.map.car_map.modules.ride.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.user.domain.User;

public interface RideRepository extends JpaRepository<Ride, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    java.util.Optional<Ride> findByIdForUpdate(@Param("id") Long id);

    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByStatusAndOfficeId(RideStatus status, Long officeId);

    List<Ride> findByStatusInAndDepartureTimeBefore(List<RideStatus> statuses, LocalDateTime time);

    List<Ride> findByDriver(User driver);
}
