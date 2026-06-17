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
import jakarta.validation.constraints.PositiveOrZero;
import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.user.domain.User;

@Entity
@Table(name = "rides")
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id")
    private Office office;

    @Enumerated(EnumType.STRING)
    private RideStatus status = RideStatus.ACTIVE;

    private String departureAddress;
    private Double departureLat;
    private Double departureLon;
    private LocalDateTime departureTime;
    private int seatsTotal;
    private int seatsAvailable;
    private String routePolyline;

    @PositiveOrZero
    private Double distanceMeters;

    @PositiveOrZero
    private Double durationSeconds;

    public Long getId() {
        return id;
    }
    public User getDriver() {
        return driver;
    }
    public void setDriver(User driver) {
        this.driver = driver;
    }
    public Office getOffice() {
        return office;
    }
    public void setOffice(Office office) {
        this.office = office;
    }
    public RideStatus getStatus() {
        return status;
    }
    public void setStatus(RideStatus status) {
        this.status = status;
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
    public int getSeatsAvailable() {
        return seatsAvailable;
    }
    public void setSeatsAvailable(int seatsAvailable) {
        this.seatsAvailable = seatsAvailable;
    }
    public String getRoutePolyline() {
        return routePolyline;
    }
    public void setRoutePolyline(String routePolyline) {
        this.routePolyline = routePolyline;
    }
    public Double getDistanceMeters() {
        return distanceMeters;
    }
    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
    public Double getDurationSeconds() {
        return durationSeconds;
    }
    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}