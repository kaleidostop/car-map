package kaleidostop.map.car_map.modules.user.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Email обязателен")
    @Email(message = "Email должен быть валидным")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Длина пароля должна быть как минимум 6 симолов")
    private String password;

    @NotBlank(message = "Полное имя обязательно")
    private String fullName;

    private String role;

    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public String getFullName() {
        return fullName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}
