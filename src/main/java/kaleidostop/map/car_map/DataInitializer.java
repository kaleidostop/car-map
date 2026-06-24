package kaleidostop.map.car_map;

import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.dto.OfficeSeed;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.RideRequest;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.dto.RideSeed;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;
import kaleidostop.map.car_map.modules.ride.repository.RideRequestRepository;
import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import kaleidostop.map.car_map.modules.routing.service.RouteService;
import kaleidostop.map.car_map.modules.routing.service.RoutingService;
import kaleidostop.map.car_map.modules.user.domain.User;
import kaleidostop.map.car_map.modules.user.domain.enums.Role;
import kaleidostop.map.car_map.modules.user.dto.UserSeed;
import kaleidostop.map.car_map.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RoutingService routingService;
    private final RouteService routeService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            loadUsers();
        }
        if (officeRepository.count() == 0) {
            loadOffices();
        }
        if (rideRepository.count() == 0) {
            List<Ride> rides = loadRides();
            fillRoutes(rides);
            createSampleRequests(rides);
        }
    }

    private void loadUsers() throws IOException {
        UserSeed[] seeds = loadJson("test-data/users.json", UserSeed[].class);
        for (UserSeed seed : seeds) {
            User user = User.fromSeed(seed, passwordEncoder);
            userRepository.save(user);
        }
    }

    private void loadOffices() throws IOException {
        OfficeSeed[] seeds = loadJson("test-data/offices.json", OfficeSeed[].class);
        for (OfficeSeed seed : seeds) {
            Office office = Office.fromSeed(seed);
            officeRepository.save(office);
        }
    }

    private List<Ride> loadRides() throws IOException {
        RideSeed[] seeds = loadJson("test-data/rides.json", RideSeed[].class);
        List<Ride> rides = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (RideSeed seed : seeds) {
            User driver = userRepository.findByEmail(seed.getDriverEmail())
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + seed.getDriverEmail()));
            Office office = officeRepository.findAll().stream()
                    .filter(o -> o.getName().equals(seed.getOfficeName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Office not found: " + seed.getOfficeName()));

            Ride ride = new Ride();
            ride.setDriver(driver);
            ride.setOffice(office);
            ride.setDepartureAddress(seed.getDepartureAddress());
            ride.setDepartureLat(seed.getDepartureLat());
            ride.setDepartureLon(seed.getDepartureLon());
            ride.setDepartureTime(now.plusMinutes(seed.getDepartureTimeOffset()));
            ride.setSeatsTotal(seed.getSeatsTotal());
            ride.setSeatsAvailable(seed.getSeatsTotal());
            ride.setStatus(RideStatus.valueOf(seed.getStatus()));
            ride.setManualApproval(true);
            rides.add(ride);
            rideRepository.save(ride);
        }
        return rides;
    }

    private void fillRoutes(List<Ride> rides) throws InterruptedException {
        for (Ride ride : rides) {
            if (ride.getRoute() == null) {
                fillRouteForRide(ride);
                Thread.sleep(1000);
            }
        }
    }

    private void createSampleRequests(List<Ride> rides) throws InterruptedException {
        List<Ride> activeRides = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.ACTIVE || r.getStatus() == RideStatus.FULL)
                .toList();
        List<User> passengers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_USER)
                .toList();
        if (activeRides.isEmpty() || passengers.isEmpty()) return;

        Random random = new Random(42);

        for (Ride ride : activeRides) {
            int requestCount = random.nextInt(3) + 1;
            for (int i = 0; i < requestCount && i < passengers.size(); i++) {
                User passenger = passengers.get(random.nextInt(passengers.size()));
                if (rideRequestRepository.existsByRideAndPassengerAndStatusIn(ride, passenger,
                        List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED))) {
                    continue;
                }

                double pLat = ride.getDepartureLat() + (random.nextDouble() - 0.5) * 0.02;
                double pLon = ride.getDepartureLon() + (random.nextDouble() - 0.5) * 0.02;

                RideRequest req = new RideRequest();
                req.setRide(ride);
                req.setPassenger(passenger);
                req.setPassengerDepartureLat(pLat);
                req.setPassengerDepartureLon(pLon);

                double r = random.nextDouble();
                if (r < 0.4 && ride.getSeatsAvailable() > 0) {
                    req.setStatus(RideRequestStatus.ACCEPTED);
                    ride.setSeatsAvailable(ride.getSeatsAvailable() - 1);
                } else if (r < 0.8) {
                    req.setStatus(RideRequestStatus.PENDING);
                } else {
                    req.setStatus(RideRequestStatus.REJECTED);
                }
                rideRequestRepository.save(req);

                // Если заявка принята, обновляем маршрут поездки
                if (req.getStatus() == RideRequestStatus.ACCEPTED) {
                    fillRouteForRide(ride);
                }

                Thread.sleep(1200);
            }

            if (ride.getSeatsAvailable() == 0 && ride.getStatus() == RideStatus.ACTIVE) {
                ride.setStatus(RideStatus.FULL);
            }
            rideRepository.save(ride);
        }
    }

    private void fillRouteForRide(Ride ride) {
        List<RideRequest> accepted = rideRequestRepository
                .findByRideIdAndStatus(ride.getId(), RideRequestStatus.ACCEPTED);

        List<double[]> waypoints = new ArrayList<>();
        for (RideRequest req : accepted) {
            if (req.getPassengerDepartureLat() != null && req.getPassengerDepartureLon() != null) {
                waypoints.add(new double[]{req.getPassengerDepartureLon(), req.getPassengerDepartureLat()});
            }
        }

        RouteInfo info;
        if (waypoints.isEmpty()) {
            info = routingService.getRoute(
                    ride.getDepartureLon(), ride.getDepartureLat(),
                    ride.getOffice().getLongitude(), ride.getOffice().getLatitude());
        } else {
            info = routingService.getRouteWithWaypoints(
                    ride.getDepartureLon(), ride.getDepartureLat(),
                    ride.getOffice().getLongitude(), ride.getOffice().getLatitude(),
                    waypoints);
        }

        if (info != null && info.getGeometry() != null) {
            Route route = routeService.createRoute(info);
            ride.setRoute(route);
            rideRepository.save(ride);
        } else {
            log.warn("Unable to build route for ride {}", ride.getId());
        }
    }


    private <T> T loadJson(String resourcePath, Class<T> type) throws IOException {
        Resource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, type);
        }
    }
}