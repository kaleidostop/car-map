package kaleidostop.map.car_map.modules.ride.service;

import kaleidostop.map.car_map.common.exception.NotFoundException;
import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.service.OfficeService;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.dto.CreateRideRequest;
import kaleidostop.map.car_map.modules.ride.dto.RideResponse;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;
import kaleidostop.map.car_map.modules.ride.repository.RideRequestRepository;
import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import kaleidostop.map.car_map.modules.routing.service.RouteService;
import kaleidostop.map.car_map.modules.routing.service.RoutingService;
import kaleidostop.map.car_map.modules.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {
    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RoutingService routingService;
    private final RouteService routeService;
    private final OfficeService officeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Ride createRide(User driver, CreateRideRequest request) {
        Office office = officeService.getById(request.getOfficeId());
        Ride ride = Ride.newRide(driver, office, request);

        RouteInfo routeInfo = routingService.getRoute(
                request.getDepartureLon(), request.getDepartureLat(),
                office.getLongitude(), office.getLatitude());
        if (routeInfo != null) {
            Route route = routeService.createRoute(routeInfo);
            ride.setRoute(route);
        }

        return rideRepository.save(ride);
    }

    @Transactional
    public void cancelRide(Long rideId, User driver) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Поездка", rideId));
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Вы не водитель этой поездки");
        }
        if (ride.getStatus() != RideStatus.ACTIVE && ride.getStatus() != RideStatus.FULL) {
            throw new IllegalStateException("Нельзя отменить поездку в текущем статусе");
        }
        ride.setStatus(RideStatus.CANCELLED);

        List<RideRequest> requestsToCancel = rideRequestRepository.findByRideIdAndStatusIn(
                rideId, List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED));
        for (RideRequest req : requestsToCancel) {
            req.setStatus(RideRequestStatus.REJECTED);

            messagingTemplate.convertAndSendToUser(
                    req.getPassenger().getEmail(),
                    "/queue/request-status",
                    Map.of("requestId", req.getId(), "rideId", rideId,
                            "status", "REJECTED", "message", "Поездка #" + rideId + " отменена водителем"));
        }
        rideRequestRepository.saveAll(requestsToCancel);
        rideRepository.save(ride);
    }

    public List<RideResponse> getActiveRides(Long officeId) {
        List<Ride> rides;
        if (officeId != null) {
            rides = rideRepository.findByStatusAndOfficeId(RideStatus.ACTIVE, officeId);
        } else {
            rides = rideRepository.findByStatus(RideStatus.ACTIVE);
        }
        return rides.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByDriver(User driver) {
        List<Ride> rides = rideRepository.findByDriver(driver);
        return rides.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getPendingRequestsCount(User driver) {
        return rideRepository.findByDriver(driver).stream()
                .mapToLong(ride -> rideRequestRepository.countByRideIdAndStatus(ride.getId(), RideRequestStatus.PENDING))
                .sum();
    }

    private RideResponse toResponse(Ride ride) {
        double distance = 0.0, duration = 0.0;
        Map<String, Object> geometry = null;
        if (ride.getRoute() != null) {
            distance = ride.getRoute().getDistanceMeters() != null ? ride.getRoute().getDistanceMeters() : 0.0;
            duration = ride.getRoute().getDurationSeconds() != null ? ride.getRoute().getDurationSeconds() : 0.0;
            if (ride.getRoute().getPolyline() != null) {
                try {
                    geometry = objectMapper.readValue(ride.getRoute().getPolyline(), Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse route geometry for ride {}", ride.getId(), e);
                }
            }
        }
        List<Map<String, Object>> passengers = rideRequestRepository
                .findByRideIdAndStatus(ride.getId(), RideRequestStatus.ACCEPTED)
                .stream()
                .map(req -> Map.of(
                        "lat", (Object) req.getPassengerDepartureLat(),
                        "lng", (Object) req.getPassengerDepartureLon(),
                        "name", req.getPassenger().getFullName()
                ))
                .collect(Collectors.toList());

        return RideResponse.from(ride, distance, duration, geometry, passengers);
    }
}