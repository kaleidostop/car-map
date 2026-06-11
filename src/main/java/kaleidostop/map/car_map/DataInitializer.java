package kaleidostop.map.car_map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import kaleidostop.map.car_map.domain.Office;
import kaleidostop.map.car_map.domain.Role;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.repository.OfficeRepository;
import kaleidostop.map.car_map.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, OfficeRepository officeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.officeRepository = officeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User driver = new User();
            driver.setEmail("driver@example.com");
            driver.setPasswordHash(passwordEncoder.encode("driver_password"));
            driver.setFullName("Корбен Даллас");
            driver.setRole(Role.ROLE_DRIVER);
            userRepository.save(driver);

            User passenger = new User();
            passenger.setEmail("user@example.com");
            passenger.setPasswordHash(passwordEncoder.encode("multipass"));
            passenger.setFullName("Лилу");
            passenger.setRole(Role.ROLE_USER);
            userRepository.save(passenger);

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
    }
}