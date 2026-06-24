package kaleidostop.map.car_map.modules.ride.service;

import kaleidostop.map.car_map.common.exception.NotFoundException;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.dto.JoinRideRequest;
import kaleidostop.map.car_map.modules.ride.dto.RideRequestDto;
import kaleidostop.map.car_map.modules.ride.dto.RideRequestPassengerDto;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;
import kaleidostop.map.car_map.modules.ride.repository.RideRequestRepository;
import kaleidostop.map.car_map.modules.ride.service.rules.RideJoinRule;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JoinRideService {
    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RoutingService routingService;
    private final RouteService routeService;
    private final List<RideJoinRule> joinRules;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> joinRide(Long rideId, User passenger, JoinRideRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Поездка", rideId));
        validateJoinRequest(ride, passenger);

        RouteInfo currentRouteInfo = RouteInfo.from(ride.getRoute(), objectMapper);
        RouteInfo newRouteInfo = buildRouteWithPassenger(ride, request.getPassengerLat(), request.getPassengerLon());
        if (newRouteInfo == null) {
            throw new RuntimeException("Не удалось рассчитать маршрут");
        }

        RideRequest rideRequest = createRideRequest(ride, passenger, request);

        if (!ride.isManualApproval()) {
            return processAutomaticMode(ride, rideRequest, currentRouteInfo, newRouteInfo);
        } else {
            return processManualMode(ride, rideRequest);
        }
    }

    @Transactional
    public Map<String, Object> handleRequest(Long rideId, Long requestId, String action, User driver) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка", requestId));
        Ride ride = request.getRide();
        if (!ride.getId().equals(rideId)) {
            throw new IllegalArgumentException("Несоответствие поездки");
        }
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Вы не водитель этой поездки");
        }
        if (!"PENDING".equals(request.getStatus().name())) {
            throw new IllegalStateException("Заявка уже обработана");
        }

        if ("accept".equals(action)) {
            if (ride.getSeatsAvailable() <= 0) {
                throw new IllegalStateException("Нет свободных мест");
            }
            request.setStatus(RideRequestStatus.ACCEPTED);
            ride.setSeatsAvailable(ride.getSeatsAvailable() - 1);
            if (ride.getSeatsAvailable() == 0) ride.setStatus(RideStatus.FULL);
            rideRepository.save(ride);

            RouteInfo newRouteInfo = buildRouteWithPassenger(ride, request.getPassengerDepartureLat(), request.getPassengerDepartureLon());
            if (newRouteInfo != null) {
                Route route = routeService.createRoute(newRouteInfo);
                ride.setRoute(route);
                rideRepository.save(ride);
            }

            notifyPassenger(request, "Ваша заявка принята");
        } else if ("reject".equals(action)) {
            request.setStatus(RideRequestStatus.REJECTED);
            notifyPassenger(request, "Ваша заявка отклонена");
        } else {
            throw new IllegalArgumentException("Неверное действие");
        }
        rideRequestRepository.save(request);
        return Map.of("status", request.getStatus().name(), "seatsAvailable", ride.getSeatsAvailable());
    }

    public List<RideRequestDto> getPendingRequests(Long rideId, User driver) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Поездка", rideId));
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Вы не водитель этой поездки");
        }

        List<RideRequest> requests = rideRequestRepository.findByRideIdAndStatus(rideId, RideRequestStatus.PENDING);
        RouteInfo currentRoute = RouteInfo.from(ride.getRoute(), objectMapper);
        double currentDistance = currentRoute != null ? currentRoute.getDistanceMeters() : 0.0;
        double currentDuration = currentRoute != null ? currentRoute.getDurationSeconds() : 0.0;

        return requests.stream()
                .map(req -> toRideRequestDto(req, ride, currentDistance, currentDuration))
                .collect(Collectors.toList());
    }

    public List<RideRequestPassengerDto> getRequestsForPassenger(User passenger) {
        List<RideRequest> requests = rideRequestRepository.findByPassengerOrderByCreatedAtDesc(passenger);
        return requests.stream().map(RideRequestPassengerDto::from)
                .collect(Collectors.toList());
    }

    private void validateJoinRequest(Ride ride, User passenger) {
        if (ride.getSeatsAvailable() <= 0 || ride.getStatus() == RideStatus.FULL) {
            throw new IllegalStateException("Нет свободных мест");
        }
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Поездка недоступна для присоединения");
        }
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("Нельзя присоединиться к своей поездке");
        }
        if (rideRequestRepository.existsByRideAndPassengerAndStatusIn(ride, passenger,
                List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED))) {
            throw new IllegalArgumentException("Вы уже отправили запрос на эту поездку");
        }
    }

    private List<double[]> collectWaypoints(Ride ride, double additionalLon, double additionalLat) {
        List<RideRequest> accepted = rideRequestRepository.findByRideIdAndStatus(ride.getId(), RideRequestStatus.ACCEPTED);
        List<double[]> waypoints = new ArrayList<>();
        for (RideRequest acc : accepted) {
            if (acc.getPassengerDepartureLat() != null && acc.getPassengerDepartureLon() != null) {
                waypoints.add(new double[]{acc.getPassengerDepartureLon(), acc.getPassengerDepartureLat()});
            }
        }
        waypoints.add(new double[]{additionalLon, additionalLat});
        return waypoints;
    }

    private RouteInfo buildRouteWithPassenger(Ride ride, double passLat, double passLon) {
        List<double[]> waypoints = collectWaypoints(ride, passLon, passLat);
        return routingService.getRouteWithWaypoints(
                ride.getDepartureLon(), ride.getDepartureLat(),
                ride.getOffice().getLongitude(), ride.getOffice().getLatitude(),
                waypoints);
    }

    private RideRequest createRideRequest(Ride ride, User passenger, JoinRideRequest request) {
        RideRequest req = RideRequest.createPending(ride, passenger, request);
        return rideRequestRepository.save(req);
    }

    private Map<String, Object> processManualMode(Ride ride, RideRequest request) {
        request.setStatus(RideRequestStatus.PENDING);
        long pendingCount = rideRequestRepository.countByRideIdAndStatus(ride.getId(), RideRequestStatus.PENDING);
        String warning = null;
        if (pendingCount >= ride.getSeatsAvailable()) {
            warning = "Внимание: количество ожидающих заявок (" + pendingCount +
                    ") уже равно или превышает число свободных мест (" +
                    ride.getSeatsAvailable() + "). Ваш запрос может быть не удовлетворён.";
        }
        rideRequestRepository.save(request);
        notifyDriver(ride, request.getPassenger(), "Новая заявка ожидает подтверждения");
        return buildJoinResponse(request, warning);
    }

    private Map<String, Object> processAutomaticMode(Ride ride, RideRequest request,
                                                     RouteInfo currentRouteInfo, RouteInfo newRouteInfo) {
        try {
            for (RideJoinRule rule : joinRules) {
                rule.check(ride, currentRouteInfo, newRouteInfo);
            }
            request.setStatus(RideRequestStatus.ACCEPTED);

            ride.setSeatsAvailable(ride.getSeatsAvailable() - 1);
            if (ride.getSeatsAvailable() == 0) ride.setStatus(RideStatus.FULL);

            Route route = routeService.createRoute(newRouteInfo);
            ride.setRoute(route);
            rideRepository.save(ride);

            notifyPassenger(request, "Ваша заявка принята автоматически");
            notifyDriver(ride, request.getPassenger(), "Новый пассажир присоединился автоматически");
        } catch (IllegalStateException e) {
            request.setStatus(RideRequestStatus.REJECTED);
            notifyPassenger(request, "Ваша заявка отклонена: " + e.getMessage());
            notifyDriver(ride, request.getPassenger(), "Заявка пассажира автоматически отклонена: " + e.getMessage());
        }
        rideRequestRepository.save(request);
        return buildJoinResponse(request, null);
    }

    private void notifyPassenger(RideRequest request, String message) {
        messagingTemplate.convertAndSendToUser(
                request.getPassenger().getEmail(),
                "/queue/request-status",
                Map.of("requestId", request.getId(), "rideId", request.getRide().getId(),
                        "status", request.getStatus().name(), "message", message));
    }

    private void notifyDriver(Ride ride, User passenger, String message) {
        messagingTemplate.convertAndSendToUser(
                ride.getDriver().getEmail(),
                "/queue/requests",
                Map.of("rideId", ride.getId(), "passengerName", passenger.getFullName(), "message", message));
    }

    private Map<String, Object> buildJoinResponse(RideRequest request, String warning) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", request.getStatus().name());
        result.put("message", request.getStatus() == RideRequestStatus.ACCEPTED ? "Заявка принята" :
                request.getStatus() == RideRequestStatus.REJECTED ? "Заявка отклонена" : "Заявка отправлена водителю");
        if (warning != null) {
            result.put("warning", warning);
        }
        return result;
    }

    private RideRequestDto toRideRequestDto(RideRequest req, Ride ride, double currentDistance, double currentDuration) {
        List<double[]> waypoints = collectWaypoints(ride, req.getPassengerDepartureLon(), req.getPassengerDepartureLat());
        RouteInfo newRoute = routingService.getRouteWithWaypoints(
                ride.getDepartureLon(), ride.getDepartureLat(),
                ride.getOffice().getLongitude(), ride.getOffice().getLatitude(),
                waypoints);

        double detourMeters = 0.0, detourMinutes = 0.0;
        if (newRoute != null) {
            detourMeters = newRoute.getDistanceMeters() - currentDistance;
            detourMinutes = (newRoute.getDurationSeconds() - currentDuration) / 60.0;
        }

        return RideRequestDto.from(req, detourMeters, detourMinutes);
    }
}