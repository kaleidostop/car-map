package kaleidostop.map.car_map.modules.ride.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;

@Component
public class RideStatusScheduler {
    private final RideRepository rideRepository;

    public RideStatusScheduler(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @Scheduled(fixedRate = 60000) // раз в минуту
    @Transactional
    public void updateRideStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Ride> toStart = rideRepository.findByStatusInAndDepartureTimeBefore(
                List.of(RideStatus.ACTIVE, RideStatus.FULL), now);
        for (Ride ride : toStart) {
            ride.setStatus(RideStatus.IN_PROGRESS);
        }
        
        List<Ride> inProgress = rideRepository.findByStatus(RideStatus.IN_PROGRESS);
        for (Ride ride : inProgress) {
            if (ride.getRoute() != null && ride.getRoute().getDurationSeconds() != null) {
                LocalDateTime finishTime = ride.getDepartureTime().plusSeconds(
                        ride.getRoute().getDurationSeconds().longValue());
                if (now.isAfter(finishTime)) {
                    ride.setStatus(RideStatus.COMPLETED);
                }
            }
        }
        rideRepository.saveAll(toStart);
        rideRepository.saveAll(inProgress);
    }
}