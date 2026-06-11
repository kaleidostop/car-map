package kaleidostop.map.car_map.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

    public Long getOfficeId() {
        return officeId;
    }
    public void setOfficeId(Long officeId) {
        this.officeId = officeId;
    }
    public String getDepartureAddress() {
        return departureAddress;
    }
    public void setDepartureAddress(String departureAddress) {
        this.departureAddress = departureAddress;
    }
    public Double getDepartureLat() {
        return departureLat;
    }
    public void setDepartureLat(Double departureLat) {
        this.departureLat = departureLat;
    }
    public Double getDepartureLon() {
        return departureLon;
    }
    public void setDepartureLon(Double departureLon) {
        this.departureLon = departureLon;
    }
    public LocalDateTime getDepartureTime() {
        return departureTime;
    }
    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }
    public int getSeatsTotal() {
        return seatsTotal;
    }
    public void setSeatsTotal(int seatsTotal) {
        this.seatsTotal = seatsTotal;
    }
}