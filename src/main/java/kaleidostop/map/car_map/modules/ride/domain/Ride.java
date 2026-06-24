package kaleidostop.map.car_map.modules.ride.domain;

import jakarta.persistence.*;
import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.dto.CreateRideRequest;
import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.user.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    private boolean manualApproval = true;
    private Double maxDetourMeters;
    private Double maxDetourSeconds;

    public static Ride newRide(User driver, Office office, CreateRideRequest request) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setOffice(office);
        ride.updateFrom(request);
        return ride;
    }

    public void updateFrom(CreateRideRequest request) {
        this.departureAddress = request.getDepartureAddress();
        this.departureLat = request.getDepartureLat();
        this.departureLon = request.getDepartureLon();
        this.departureTime = request.getDepartureTime();
        this.seatsTotal = request.getSeatsTotal();
        this.seatsAvailable = request.getSeatsTotal();
        this.status = RideStatus.ACTIVE;
        this.manualApproval = request.getManualApproval() != null ? request.getManualApproval() : true;
        if (request.getMaxDetourMeters() != null) {
            this.setMaxDetourMeters(request.getMaxDetourMeters());
        }
        if (request.getMaxDetourMinutes() != null) {
            this.setMaxDetourSeconds(request.getMaxDetourMinutes() * 60L);
        }
    }
}