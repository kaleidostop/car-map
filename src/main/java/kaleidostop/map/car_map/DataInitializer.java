package kaleidostop.map.car_map;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;
import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import kaleidostop.map.car_map.modules.routing.service.RouteService;
import kaleidostop.map.car_map.modules.routing.service.RoutingService;
import kaleidostop.map.car_map.modules.user.domain.Role;
import kaleidostop.map.car_map.modules.user.domain.User;
import kaleidostop.map.car_map.modules.user.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final RideRepository rideRepository;
    private final RoutingService routingService;
    private final RouteService routeService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, OfficeRepository officeRepository, RideRepository rideRepository, RoutingService routingService, RouteService routeService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.officeRepository = officeRepository;
        this.rideRepository = rideRepository;
        this.routingService = routingService;
        this.routeService = routeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Водители
            User driver1 = new User();
            driver1.setEmail("driver@example.com");
            driver1.setPasswordHash(passwordEncoder.encode("driver"));
            driver1.setFullName("Райан Гослинг");
            driver1.setRole(Role.ROLE_DRIVER);
            userRepository.save(driver1);

            User driver2 = new User();
            driver2.setEmail("car@example.com");
            driver2.setPasswordHash(passwordEncoder.encode("car"));
            driver2.setFullName("Молния Маккуин");
            driver2.setRole(Role.ROLE_DRIVER);
            userRepository.save(driver2);

            User driver3 = new User();
            driver3.setEmail("transformer@example.com");
            driver3.setPasswordHash(passwordEncoder.encode("transformer"));
            driver3.setFullName("Оптимус Прайм");
            driver3.setRole(Role.ROLE_DRIVER);
            userRepository.save(driver3);

            // Пассажиры
            User passenger1 = new User();
            passenger1.setEmail("user1@example.com");
            passenger1.setPasswordHash(passwordEncoder.encode("user1"));
            passenger1.setFullName("Пассажир 1");
            passenger1.setRole(Role.ROLE_USER);
            userRepository.save(passenger1);

            User passenger2 = new User();
            passenger2.setEmail("user2@example.com");
            passenger2.setPasswordHash(passwordEncoder.encode("user2"));
            passenger2.setFullName("Пассажир 2");
            passenger2.setRole(Role.ROLE_USER);
            userRepository.save(passenger2);

            User passenger3 = new User();
            passenger3.setEmail("user3@example.com");
            passenger3.setPasswordHash(passwordEncoder.encode("user3"));
            passenger3.setFullName("Пассажир 3");
            passenger3.setRole(Role.ROLE_USER);
            userRepository.save(passenger3);

            // Админ
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPasswordHash(passwordEncoder.encode("admin"));
            admin.setFullName("Админ");
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
        }
        if (officeRepository.count() == 0) {
            Office o1 = new Office();
            o1.setName("Главный корпус");
            o1.setAddress("Санкт-Петербург, Кронверкский пр., 49");
            o1.setLatitude(59.956363);
            o1.setLongitude(30.310011);
            officeRepository.save(o1);

            Office o2 = new Office();
            o2.setName("Корпус на Ломоносова");
            o2.setAddress("Санкт-Петербург, ул. Ломоносова, 9");
            o2.setLatitude(59.927288);
            o2.setLongitude(30.338353);
            officeRepository.save(o2);

            Office o3 = new Office();
            o3.setName("Спортивный комплекс");
            o3.setAddress("Санкт-Петербург, Вяземский пер., 5-7");
            o3.setLatitude(59.972631);
            o3.setLongitude(30.302501);
            officeRepository.save(o3);
        }

        List<Ride> ridesWithoutRoute = rideRepository.findAll()
        .stream()
        .filter(r -> r.getRoute() == null)
        .toList();

        try {
            for (Ride ride : ridesWithoutRoute) {
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
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
            }
    }
}