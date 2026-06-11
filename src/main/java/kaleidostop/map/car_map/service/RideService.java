package kaleidostop.map.car_map.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kaleidostop.map.car_map.domain.Office;
import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideStatus;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.repository.OfficeRepository;
import kaleidostop.map.car_map.repository.RideRepository;

@Service
public class RideService {
    private final RideRepository rideRepository;
    private final OfficeRepository officeRepository;

    public RideService(RideRepository rideRepository, OfficeRepository officeRepository) {
        this.rideRepository = rideRepository;
        this.officeRepository = officeRepository;
    }

    @Transactional
    public Ride createRide(User driver, Long officeId, String departureAddress,
                           Double depLat, Double depLon,
                           LocalDateTime departureTime, int seatsTotal) {
        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new IllegalArgumentException("Office not found"));

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setOffice(office);
        ride.setDepartureAddress(departureAddress);
        ride.setDepartureLat(depLat);
        ride.setDepartureLon(depLon);
        ride.setDepartureTime(departureTime);
        ride.setSeatsTotal(seatsTotal);
        ride.setSeatsAvailable(seatsTotal);
        ride.setStatus(RideStatus.valueOf("ACTIVE"));
        // Пока без маршрута, расстояния и т.д.
        return rideRepository.save(ride);
    }
}