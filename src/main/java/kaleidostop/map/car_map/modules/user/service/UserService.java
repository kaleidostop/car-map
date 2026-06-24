package kaleidostop.map.car_map.modules.user.service;

import kaleidostop.map.car_map.common.exception.NotFoundException;
import kaleidostop.map.car_map.common.exception.UserAlreadyExistsException;
import kaleidostop.map.car_map.modules.user.domain.User;
import kaleidostop.map.car_map.modules.user.domain.enums.Role;
import kaleidostop.map.car_map.modules.user.dto.RegisterRequest;
import kaleidostop.map.car_map.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(resolveRole(request.getRole()));
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь", email));
    }

    private Role resolveRole(String roleStr) {
        try {
            Role role = Role.valueOf(roleStr);
            if (role == Role.ROLE_ADMIN) {
                throw new IllegalArgumentException("Регистрация с ролью администратора запрещена");
            }
            return role;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимая роль: " + roleStr);
        }
    }
}