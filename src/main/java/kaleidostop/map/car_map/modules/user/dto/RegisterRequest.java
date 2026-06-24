package kaleidostop.map.car_map.modules.user.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Email обязателен")
    @Email(message = "Email должен быть валидным")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Длина пароля должна быть как минимум 6 симолов")
    private String password;

    @NotBlank(message = "Полное имя обязательно")
    private String fullName;

    @NotBlank(message = "Роль обязательна")
    private String role;
}
