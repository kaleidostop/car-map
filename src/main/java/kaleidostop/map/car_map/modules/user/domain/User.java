package kaleidostop.map.car_map.modules.user.domain;

import jakarta.persistence.*;
import kaleidostop.map.car_map.modules.user.domain.enums.Role;
import kaleidostop.map.car_map.modules.user.dto.UserSeed;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    public static User fromSeed(UserSeed seed, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setEmail(seed.getEmail());
        user.setPasswordHash(passwordEncoder.encode(seed.getPassword()));
        user.setFullName(seed.getFullName());
        user.setRole(Role.valueOf(seed.getRole()));
        return user;
    }
}