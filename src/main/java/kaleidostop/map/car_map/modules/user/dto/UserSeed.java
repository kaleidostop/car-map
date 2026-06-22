package kaleidostop.map.car_map.modules.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSeed {
    private String email;
    private String password;
    private String fullName;
    private String role;
}