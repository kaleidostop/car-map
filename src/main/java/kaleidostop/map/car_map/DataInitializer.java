package kaleidostop.map.car_map;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import tools.jackson.databind.ObjectMapper;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RoutingService routingService;
    private final RouteService routeService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataInitializer(UserRepository userRepository, OfficeRepository officeRepository, RideRepository rideRepository, RideRequestRepository rideRequestRepository, RoutingService routingService, RouteService routeService, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.officeRepository = officeRepository;
        this.rideRepository = rideRepository;
        this.rideRequestRepository = rideRequestRepository;
        this.routingService = routingService;
        this.routeService = routeService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

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
            User user = new User();
            user.setEmail(seed.getEmail());
            user.setPasswordHash(passwordEncoder.encode(seed.getPassword()));
            user.setFullName(seed.getFullName());
            user.setRole(Role.valueOf(seed.getRole()));
            userRepository.save(user);
        }
    }

    private void loadOffices() throws IOException {
        OfficeSeed[] seeds = loadJson("test-data/offices.json", OfficeSeed[].class);
        for (OfficeSeed seed : seeds) {
            Office office = new Office();
            office.setName(seed.getName());
            office.setAddress(seed.getAddress());
            office.setLatitude(seed.getLatitude());
            office.setLongitude(seed.getLongitude());
            officeRepository.save(office);
        }
    }

    private List<Ride> loadRides() throws IOException {
        RideSeed[] seeds = loadJson("test-data/rides.json", RideSeed[].class);
        List<Ride> rides = new ArrayList<>();
        for (RideSeed seed : seeds) {
            User driver = userRepository.findByEmail(seed.getDriverEmail())
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + seed.getDriverEmail()));
            Office office = officeRepository.findAll().stream()
                    .filter(o -> o.getName().equals(seed.getOfficeName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Office not found: " + seed.getOfficeName()));

            LocalDateTime now = LocalDateTime.now();
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
            rides.add(ride);
            rideRepository.save(ride);
        }
        return rides;
    }

    private void fillRoutes(List<Ride> rides) {
        for (Ride ride : rides) {
            if (ride.getRoute() == null) {
                RouteInfo info = routingService.getRoute(
                        ride.getDepartureLon(), ride.getDepartureLat(),
                        ride.getOffice().getLongitude(), ride.getOffice().getLatitude()
                );
                if (info != null && info.getGeometry() != null) {
                    Route route = routeService.createRoute(
                            info.getGeometry(),
                            info.getDistanceMeters(),
                            info.getDurationSeconds()
                    );
                    ride.setRoute(route);
                    rideRepository.save(ride);
                }
            }
        }
    }


    private void createSampleRequests(List<Ride> rides) {
        List<Ride> activeRides = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.ACTIVE || r.getStatus() == RideStatus.FULL)
                .collect(Collectors.toList());
        List<User> passengers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_USER)
                .collect(Collectors.toList());

        if (activeRides.isEmpty() || passengers.isEmpty()) return;

        Random random = new Random(42); 

        for (Ride ride : activeRides) {
            int requestCount = random.nextInt(3) + 1; // 1..3
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
            }
            if (ride.getSeatsAvailable() == 0 && ride.getStatus() == RideStatus.ACTIVE) {
                ride.setStatus(RideStatus.FULL);
            }
            rideRepository.save(ride);
        }
    }

    private <T> T loadJson(String resourcePath, Class<T> type) throws IOException {
        Resource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, type);
        }
    }
}