package kaleidostop.map.car_map.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kaleidostop.map.car_map.domain.Office;
import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideStatus;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.dto.RideResponse;
import kaleidostop.map.car_map.dto.osrm.RouteInfo;
import kaleidostop.map.car_map.repository.OfficeRepository;
import kaleidostop.map.car_map.repository.RideRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class RideService {
    private final RideRepository rideRepository;
    private final OfficeRepository officeRepository;
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    public RideService(RideRepository rideRepository, OfficeRepository officeRepository, RoutingService routingService, ObjectMapper objectMapper) {
        this.rideRepository = rideRepository;
        this.officeRepository = officeRepository;
        this.routingService = routingService;
        this.objectMapper = objectMapper;
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
        ride.setStatus(RideStatus.ACTIVE);

        RouteInfo routeInfo = routingService.getRoute(
            depLon, depLat, office.getLongitude(), office.getLatitude());
        if (routeInfo != null) {
            ride.setDistanceMeters(routeInfo.getDistanceMeters());
            ride.setDurationSeconds(routeInfo.getDurationSeconds());
            ride.setRoutePolyline(toJson(routeInfo.getGeometry())); 
        } else {
            ride.setDistanceMeters(0.0);
            ride.setDurationSeconds(0.0);
        }

        return rideRepository.save(ride);
    }

    public List<RideResponse> getActiveRides(Long officeId) {
        List<Ride> rides;
        if (officeId != null) {
            rides = rideRepository.findByStatusAndOfficeId(RideStatus.ACTIVE, officeId);
        } else {
            rides = rideRepository.findByStatus(RideStatus.ACTIVE);
        }
        return rides.stream().map(this::toResponse).toList();
    }


    @SuppressWarnings("unchecked")
    private RideResponse toResponse(Ride ride) {
        Map<String, Object> geometry = null;
        if (ride.getRoutePolyline() != null) {
            try {
                geometry = objectMapper.readValue(
                    ride.getRoutePolyline(),
                    Map.class
                );
            } catch (Exception e) {
                log.warn("Failed to parse route geometry for ride {}", ride.getId(), e);
            }
        }

        return new RideResponse(
            ride.getId(),
            ride.getDriver().getFullName(), 
            ride.getOffice().getName(),
            ride.getOffice().getLatitude(),
            ride.getOffice().getLongitude(),
            ride.getDepartureAddress(),
            ride.getDepartureLat(),
            ride.getDepartureLon(),
            ride.getDepartureTime(),
            ride.getSeatsTotal(),
            ride.getSeatsAvailable(),
            ride.getStatus().name(),
            ride.getDistanceMeters() != null ? ride.getDistanceMeters() : 0.0,
            ride.getDurationSeconds() != null ? ride.getDurationSeconds() : 0.0,
            geometry
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}