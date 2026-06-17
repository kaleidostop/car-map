package kaleidostop.map.car_map.modules.ride.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kaleidostop.map.car_map.modules.user.domain.User;

@Entity
@Table(name = "ride_requests")
public class RideRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private User passenger;

    @Enumerated(EnumType.STRING)
    private RideRequestStatus status;

    private Double passengerDepartureLat;
    private Double passengerDepartureLon;
    private String passengerDepartureAddress;
    private LocalDateTime createdAt = LocalDateTime.now();


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public User getPassenger() {
        return passenger;
    }

    public void setPassenger(User passenger) {
        this.passenger = passenger;
    }

    public RideRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RideRequestStatus status) {
        this.status = status;
    }

    public Double getPassengerDepartureLat() {
        return passengerDepartureLat;
    }

    public void setPassengerDepartureLat(Double passengerDepartureLat) {
        this.passengerDepartureLat = passengerDepartureLat;
    }

    public Double getPassengerDepartureLon() {
        return passengerDepartureLon;
    }

    public void setPassengerDepartureLon(Double passengerDepartureLon) {
        this.passengerDepartureLon = passengerDepartureLon;
    }

    public String getPassengerDepartureAddress() {
        return passengerDepartureAddress;
    }

    public void setPassengerDepartureAddress(String passengerDepartureAddress) {
        this.passengerDepartureAddress = passengerDepartureAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}