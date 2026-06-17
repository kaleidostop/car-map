package kaleidostop.map.car_map.modules.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kaleidostop.map.car_map.common.security.JwtUtil;
import kaleidostop.map.car_map.modules.user.domain.Role;
import kaleidostop.map.car_map.modules.user.domain.User;
import kaleidostop.map.car_map.modules.user.dto.LoginRequest;
import kaleidostop.map.car_map.modules.user.dto.RegisterRequest;
import kaleidostop.map.car_map.modules.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, JwtUtil jwtUtil,
                         AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            User user = userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                Role.ROLE_USER 
            );
            String token = jwtUtil.generateToken(user);
            return ResponseEntity.ok(Map.of("token", token, "email", user.getEmail(), "role", user.getRole().name()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userService.findByEmail(request.getEmail());
            String token = jwtUtil.generateToken(user);
            return ResponseEntity.ok(Map.of("token", token, "email", user.getEmail(), "role", user.getRole().name()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }
}