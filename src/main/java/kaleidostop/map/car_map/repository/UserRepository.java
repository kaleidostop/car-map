package kaleidostop.map.car_map.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}