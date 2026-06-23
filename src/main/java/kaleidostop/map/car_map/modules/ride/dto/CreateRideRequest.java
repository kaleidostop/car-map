package kaleidostop.map.car_map.modules.ride.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateRideRequest {

    @NotNull(message = "Офис обязателен")
    private Long officeId;

    @NotBlank(message = "Адрес отправления обязателен")
    private String departureAddress;

    @NotNull(message = "Широта обязательна")
    @DecimalMin(value = "-90.0", message = "Широта должна быть >= -90")
    @DecimalMax(value = "90.0", message = "Широта должна быть <= 90")
    private Double departureLat;

    @NotNull(message = "Долгота обязательна")
    @DecimalMin(value = "-180.0", message = "Долгота должна быть >= -180")
    @DecimalMax(value = "180.0", message = "Долгота должна быть <= 180")
    private Double departureLon;

    @NotNull(message = "Время отправления обязательно")
    @FutureOrPresent(message = "Время отправления должно быть в настоящем или будущем")
    private LocalDateTime departureTime;

    @Positive(message = "Количество мест должно быть > 0")
    private int seatsTotal;

    private Double maxDetourMeters;
    private Double maxDetourMinutes;
}