package kaleidostop.map.car_map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import kaleidostop.map.car_map.domain.Role;
import kaleidostop.map.car_map.domain.User;
import kaleidostop.map.car_map.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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
            passenger.setPasswordHash(passwordEncoder.encode("user_password"));
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
    }
}