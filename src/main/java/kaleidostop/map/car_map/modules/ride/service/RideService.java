package kaleidostop.map.car_map.modules.ride.service;

import kaleidostop.map.car_map.common.exception.NotFoundException;
import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.service.OfficeService;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.dto.RideRequestDto;
import kaleidostop.map.car_map.modules.ride.dto.RideRequestPassengerDto;
import kaleidostop.map.car_map.modules.ride.dto.RideResponse;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;
import kaleidostop.map.car_map.modules.ride.repository.RideRequestRepository;
import kaleidostop.map.car_map.modules.ride.service.rules.RideJoinRule;
import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import kaleidostop.map.car_map.modules.routing.service.RouteService;
import kaleidostop.map.car_map.modules.routing.service.RoutingService;
import kaleidostop.map.car_map.modules.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RideService {
    private final RideRepository rideRepository;
    private final OfficeService officeService;
    private final RoutingService routingService;
    private final RouteService routeService;
    private final ObjectMapper objectMapper;
    private final RideRequestRepository rideRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final List<RideJoinRule> joinRules;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    public RideService(RideRepository rideRepository, OfficeService officeService, RoutingService routingService, RouteService routeService, RideRequestRepository rideRequestRepository, SimpMessagingTemplate messagingTemplate, List<RideJoinRule> joinRules, ObjectMapper objectMapper) {
        this.rideRepository = rideRepository;
        this.officeService = officeService;
        this.routingService = routingService;
        this.routeService = routeService;
        this.objectMapper = objectMapper;
        this.rideRequestRepository = rideRequestRepository;
        this.joinRules = joinRules;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Ride createRide(User driver, Long officeId, String departureAddress,
                           Double depLat, Double depLon,
                           LocalDateTime departureTime, int seatsTotal) {
        Office office = officeService.getById(officeId);

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
        if (routeInfo != null && routeInfo.getGeometry() != null) {
            Route route = routeService.createRoute(
                routeInfo.getGeometry(),
                routeInfo.getDistanceMeters(),
                routeInfo.getDurationSeconds()
            );
            ride.setRoute(route);
        } else {
            ride.setRoute(null);
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

    @Transactional
    public Map<String, Object> joinRide(Long rideId, User passenger, double passLat, double passLon) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Поездка", rideId));

        if (ride.getSeatsAvailable() <= 0 || ride.getStatus() == RideStatus.FULL) {
            throw new IllegalStateException("Нет свободных мест");
        }
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Поездка недоступна для присоединения");
        }
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("Нельзя присоединиться к своей поездке");
        }
        if (rideRequestRepository.existsByRideAndPassengerAndStatusIn(ride, passenger, List.of(RideRequestStatus.ACCEPTED, RideRequestStatus.PENDING))) {
            throw new IllegalArgumentException("Вы уже отправили запрос на эту поездку");
        }

        RouteInfo currentRouteInfo = toRouteInfo(ride.getRoute());
        double currentDistance = currentRouteInfo != null ? currentRouteInfo.getDistanceMeters() : 0.0;
        double currentDuration = currentRouteInfo != null ? currentRouteInfo.getDurationSeconds() : 0.0;

        RouteInfo newRouteInfo = buildRouteWithNewPassenger(ride, passenger, passLat, passLon);
        if (newRouteInfo == null) {
            throw new RuntimeException("Не удалось рассчитать маршрут");
        }

        RideRequest request = new RideRequest();
        request.setRide(ride);
        request.setPassenger(passenger);
        request.setPassengerDepartureLat(passLat);
        request.setPassengerDepartureLon(passLon);
        Route newRoute = routeService.createRoute(newRouteInfo.getGeometry(),
                newRouteInfo.getDistanceMeters(), newRouteInfo.getDurationSeconds());
        request.setRoute(newRoute);

        String warning = null;
        String messageToDriver = null;

        if (ride.isManualApproval()) {
            request.setStatus(RideRequestStatus.PENDING);

            long pendingCount = rideRequestRepository.countByRideIdAndStatus(rideId, RideRequestStatus.PENDING);
            if (pendingCount >= ride.getSeatsAvailable()) {
                warning = "Внимание: количество ожидающих заявок (" + pendingCount +
                        ") уже равно или превышает число свободных мест (" +
                        ride.getSeatsAvailable() + "). Ваш запрос может быть не удовлетворён.";
            }
            messageToDriver = "Новый запрос от " + passenger.getFullName();
        } else {
            try {
                for (RideJoinRule rule : joinRules) {
                    rule.check(ride, currentRouteInfo, newRouteInfo);
                }
                request.setStatus(RideRequestStatus.ACCEPTED);

                ride.setSeatsAvailable(ride.getSeatsAvailable() - 1);
                if (ride.getSeatsAvailable() == 0) ride.setStatus(RideStatus.FULL);

                ride.setRoute(newRoute);
                rideRepository.save(ride);

                String passengerEmail = request.getPassenger().getEmail();
                messagingTemplate.convertAndSendToUser(
                        passengerEmail,
                        "/queue/request-status",
                        Map.of(
                                "requestId", request.getId(),
                                "rideId", ride.getId(),
                                "status", request.getStatus(),
                                "message", "Ваша заявка на поездку #" + ride.getId() + " принята автоматически!"
                        )
                );
                messageToDriver =  "Новый пассажир " + passenger.getFullName() + " присоединился к поездке #" + ride.getId() + " (автоматически)";
            } catch (IllegalStateException e) {
                request.setStatus(RideRequestStatus.REJECTED);

                String passengerEmail = request.getPassenger().getEmail();
                messagingTemplate.convertAndSendToUser(
                        passengerEmail,
                        "/queue/request-status",
                        Map.of(
                                "requestId", request.getId(),
                                "rideId", ride.getId(),
                                "status", request.getStatus(),
                                "message", "Ваша заявка на поездку #" + ride.getId() + " отклонена:" + e.getMessage()
                        )
                );
                messageToDriver = "Заявка от " + passenger.getFullName() + " автоматически отклонена: " + e.getMessage();
            }
        }

        rideRequestRepository.save(request);

        messagingTemplate.convertAndSendToUser(
                ride.getDriver().getEmail(),
                "/queue/requests",
                Map.of(
                        "rideId", ride.getId(),
                        "passengerName", passenger.getFullName(),
                        "message", messageToDriver
                )
        );

        Map<String, Object> result = new HashMap<>();
        result.put("status", request.getStatus().name());
        result.put("message", request.getStatus() == RideRequestStatus.ACCEPTED
                ? "Заявка принята" :
                request.getStatus() == RideRequestStatus.REJECTED
                ? "Заявка отклонена: превышены лимиты" : "Заявка отправлена водителю");
        if (warning != null) {
            result.put("warning", warning);
        }
        return result;
    }

    public List<RideRequestDto> getPendingRequests(Long rideId, User driver) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Поездка", rideId));
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Вы не водитель этой поездки");
        }

        List<RideRequest> requests = rideRequestRepository.findByRideIdAndStatus(rideId, RideRequestStatus.PENDING);

        Route currentRoute = ride.getRoute();
        double currentDistance = currentRoute != null && currentRoute.getDistanceMeters() != null
                ? currentRoute.getDistanceMeters() : 0.0;
        double currentDuration = currentRoute != null && currentRoute.getDurationSeconds() != null
                ? currentRoute.getDurationSeconds() : 0.0;

        return requests.stream()
                .map(r -> {
                    double newDistance = 0.0;
                    double newDuration = 0.0;
                    Route requestRoute = r.getRoute();
                    if (requestRoute != null) {
                        newDistance = requestRoute.getDistanceMeters() != null ? requestRoute.getDistanceMeters() : 0.0;
                        newDuration = requestRoute.getDurationSeconds() != null ? requestRoute.getDurationSeconds() : 0.0;
                    }
                    double detourMeters = newDistance - currentDistance;
                    double detourSeconds = newDuration - currentDuration;
                    double detourMinutes = detourSeconds / 60.0;

                    return new RideRequestDto(
                            r.getId(),
                            r.getPassenger().getFullName(),
                            r.getPassengerDepartureLat() != null ? r.getPassengerDepartureLat() : 0.0,
                            r.getPassengerDepartureLon() != null ? r.getPassengerDepartureLon() : 0.0,
                            detourMeters,
                            detourMinutes
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> handleRequest(Long rideId, Long requestId, String action, User driver) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Поездка", rideId));
        Ride ride = request.getRide();
        if (!ride.getId().equals(rideId)) {
            throw new IllegalArgumentException("Несоответствие поездки");
        }
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Вы не водитель этой поездки");
        }
        if (!RideRequestStatus.PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Заявка уже обработана");
        }
        if ("accept".equals(action)) {
            if (ride.getSeatsAvailable() <= 0) {
                throw new IllegalStateException("Нет свободных мест");
            }
            request.setStatus(RideRequestStatus.ACCEPTED);
            ride.setSeatsAvailable(ride.getSeatsAvailable() - 1);
            if (ride.getSeatsAvailable() == 0) {
                ride.setStatus(RideStatus.FULL);
            }
            ride.setRoute(request.getRoute());
            ride.setSeatsAvailable(ride.getSeatsAvailable() - 1);
            if (ride.getSeatsAvailable() == 0) {
                ride.setStatus(RideStatus.FULL);
            }
            rideRepository.save(ride);

        } else if ("reject".equals(action)) {
            request.setStatus(RideRequestStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Неверное действие");
        }
        rideRequestRepository.save(request);

        String passengerEmail = request.getPassenger().getEmail();
        String message = action.equals("accept") 
                ? "Ваша заявка на поездку #" + ride.getId() + " принята!"
                : "Ваша заявка на поездку #" + ride.getId() + " отклонена.";
        messagingTemplate.convertAndSendToUser(
            passengerEmail,
            "/queue/request-status",
            Map.of(
                "requestId", request.getId(),
                "rideId", ride.getId(),
                "status", request.getStatus(),
                "message", message
            )
        );

        return Map.of("status", request.getStatus(), "seatsAvailable", ride.getSeatsAvailable());
    }

    public List<RideRequestPassengerDto> getRequestsForPassenger(User passenger) {
        List<RideRequest> requests = rideRequestRepository.findByPassengerOrderByCreatedAtDesc(passenger);
        return requests.stream().map(r -> {
            Ride ride = r.getRide();
            return new RideRequestPassengerDto(
                    r.getId(),
                    ride.getId(),
                    ride.getDepartureAddress(),
                    ride.getOffice().getName(),
                    ride.getDepartureTime(),
                    r.getStatus(),
                    r.getPassengerDepartureLat() != null ? r.getPassengerDepartureLat() : 0.0,
                    r.getPassengerDepartureLon() != null ? r.getPassengerDepartureLon() : 0.0,
                    ride.getDriver().getFullName(),
                    ride.getSeatsAvailable()
            );
        }).collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByDriver(User driver) {
        List<Ride> rides = rideRepository.findByDriver(driver);
        return rides.stream().map(this::toResponse).toList();
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
                rideId,
                List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED)
        );
        for (RideRequest req : requestsToCancel) {
            req.setStatus(RideRequestStatus.REJECTED);
            messagingTemplate.convertAndSendToUser(
                    req.getPassenger().getEmail(),
                    "/queue/request-status",
                    Map.of(
                            "requestId", req.getId(),
                            "rideId", rideId,
                            "status", "REJECTED",
                            "message", "Поездка #" + rideId + " была отменена водителем"
                    )
            );
        }
        rideRepository.save(ride);
    }

    public long getPendingRequestsCount(User driver) {
        return rideRepository.findByDriver(driver).stream()
                .mapToLong(ride -> rideRequestRepository.countByRideIdAndStatus(ride.getId(), RideRequestStatus.PENDING))
                .sum();
    }

    @SuppressWarnings("unchecked")
    private RideResponse toResponse(Ride ride) {
        Map<String, Object> geometry = null;
        double distance = 0.0;
        double duration = 0.0;
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

        List<RideRequest> acceptedRequests = rideRequestRepository.findByRideIdAndStatus(ride.getId(), RideRequestStatus.ACCEPTED);

        List<Map<String, Object>> passengers = new ArrayList<>();
        for (RideRequest req : acceptedRequests) {
            Map<String, Object> p = new HashMap<>();
            p.put("lat", req.getPassengerDepartureLat());
            p.put("lng", req.getPassengerDepartureLon());
            p.put("name", req.getPassenger().getFullName());
            passengers.add(p);
        }

        return new RideResponse(
            ride.getId(),
            ride.getDriver().getFullName(), 
            ride.getDriver().getEmail(),
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
            distance,
            duration,
            geometry,
            passengers
        );
    }

    private RouteInfo toRouteInfo(Route route) {
        if (route == null) return null;
        try {
            RouteInfo info = new RouteInfo();
            info.setDistanceMeters(route.getDistanceMeters());
            info.setDurationSeconds(route.getDurationSeconds());
            info.setGeometry(objectMapper.readValue(route.getPolyline(), Map.class));
            return info;
        } catch (Exception e) {
            log.warn("Failed to deserialize route {}", route.getId(), e);
            return null;
        }
    }

    private RouteInfo buildRouteWithNewPassenger(Ride ride, User passenger, double passLat, double passLon) {
        List<RideRequest> accepted = rideRequestRepository.findByRideIdAndStatus(ride.getId(), RideRequestStatus.ACCEPTED);
        List<double[]> waypoints = new ArrayList<>();
        for (RideRequest req : accepted) {
            if (req.getPassengerDepartureLat() != null && req.getPassengerDepartureLon() != null) {
                waypoints.add(new double[]{req.getPassengerDepartureLon(), req.getPassengerDepartureLat()});
            }
        }
        waypoints.add(new double[]{passLon, passLat});
        return routingService.getRouteWithWaypoints(
                ride.getDepartureLon(), ride.getDepartureLat(),
                ride.getOffice().getLongitude(), ride.getOffice().getLatitude(),
                waypoints);
    }

}