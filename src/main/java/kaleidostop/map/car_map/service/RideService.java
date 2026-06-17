package kaleidostop.map.car_map.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kaleidostop.map.car_map.config.RideConstants;
import kaleidostop.map.car_map.domain.Office;
import kaleidostop.map.car_map.domain.Ride;
import kaleidostop.map.car_map.domain.RideRequest;
import kaleidostop.map.car_map.domain.RideRequestStatus;
import kaleidostop.map.car_map.domain.RideStatus;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.dto.RideRequestDto;
import kaleidostop.map.car_map.dto.RideRequestPassengerDto;
import kaleidostop.map.car_map.dto.RideResponse;
import kaleidostop.map.car_map.dto.osrm.RouteInfo;
import kaleidostop.map.car_map.repository.OfficeRepository;
import kaleidostop.map.car_map.repository.RideRepository;
import kaleidostop.map.car_map.repository.RideRequestRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class RideService {
    private final RideRepository rideRepository;
    private final OfficeRepository officeRepository;
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;
    private final RideRequestRepository rideRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    public RideService(RideRepository rideRepository, OfficeRepository officeRepository, RoutingService routingService, RideRequestRepository rideRequestRepository, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.rideRepository = rideRepository;
        this.officeRepository = officeRepository;
        this.routingService = routingService;
        this.objectMapper = objectMapper;
        this.rideRequestRepository = rideRequestRepository;
        this.messagingTemplate = messagingTemplate;
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

    @Transactional
    public Map<String, Object> joinRide(Long rideId, User passenger, double passLat, double passLon) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Поездка не найдена"));

        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Поездка недоступна для присоединения");
        }
        if (ride.getSeatsAvailable() <= 0) {
            throw new IllegalStateException("Нет свободных мест");
        }
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("Нельзя присоединиться к своей поездке");
        }
        if (rideRequestRepository.existsByRideAndPassengerAndStatusIn(ride, passenger, List.of(RideRequestStatus.ACCEPTED, RideRequestStatus.PENDING))) {
            throw new IllegalArgumentException("Вы уже отправили запрос на эту поездку");
        }

        double originalDistance = ride.getDistanceMeters() != null ? ride.getDistanceMeters() : 0.0;
        RouteInfo detourInfo = routingService.getRoute(
                ride.getDepartureLon(), ride.getDepartureLat(),
                passLon, passLat,
                ride.getOffice().getLongitude(), ride.getOffice().getLatitude()
        );
        if (detourInfo == null) {
            throw new RuntimeException("Не удалось рассчитать маршрут с пассажиром");
        }

        double newDistance = detourInfo.getDistanceMeters();
        double detour = newDistance - originalDistance;
        if (detour > RideConstants.MAX_DETOUR_METERS) {
            RideRequest rejected = new RideRequest();
            rejected.setRide(ride);
            rejected.setPassenger(passenger);
            rejected.setStatus(RideRequestStatus.REJECTED);
            rejected.setPassengerDepartureLat(passLat);
            rejected.setPassengerDepartureLon(passLon);

            rideRequestRepository.save(rejected);

            return Map.of(
                "status", "REJECTED",
                "message", "Запрос отклонён: превышено допустимое добавочное расстояние"
            );
        }

        long pendingCount = rideRequestRepository.countByRideIdAndStatus(rideId, RideRequestStatus.PENDING);
        String warning = null;
        if (pendingCount >= ride.getSeatsAvailable()) {
            warning = "Внимание: количество ожидающих заявок (" + pendingCount +
                      ") уже равно или превышает число свободных мест (" +
                      ride.getSeatsAvailable() + "). Ваш запрос может быть не удовлетворён.";
        }

        RideRequest pending = new RideRequest();
        pending.setRide(ride);
        pending.setPassenger(passenger);
        pending.setStatus(RideRequestStatus.PENDING);
        pending.setPassengerDepartureLat(passLat);
        pending.setPassengerDepartureLon(passLon);
        rideRequestRepository.save(pending);

        messagingTemplate.convertAndSendToUser(
                ride.getDriver().getEmail(),
                "/queue/requests",
                Map.of(
                    "rideId", ride.getId(),
                    "passengerName", passenger.getFullName(),
                    "message", "Новый запрос от " + passenger.getFullName()
                )
        );

        Map<String, Object> result = new HashMap<>();
        result.put("status", "PENDING");
        result.put("message", "Заявка отправлена водителю");
        if (warning != null) {
            result.put("warning", warning);
        }
        return result;
    }

    public List<RideRequestDto> getPendingRequests(Long rideId, User driver) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Поездка не найдена"));
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Вы не водитель этой поездки");
        }
        List<RideRequest> requests = rideRequestRepository.findPendingRequestsByRideId(rideId);
        return requests.stream()
                .map(r -> new RideRequestDto(r.getId(), r.getPassenger().getFullName(),
                        r.getPassengerDepartureLat(), r.getPassengerDepartureLon()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> handleRequest(Long rideId, Long requestId, String action, User driver) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
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