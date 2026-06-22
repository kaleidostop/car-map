package kaleidostop.map.car_map.modules.ride.dto;

import java.time.LocalDateTime;

import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import lombok.Getter;

@Getter
public class RideRequestPassengerDto {
    private Long id;
    private Long rideId;
    private String departureAddress; 
    private String officeName;       
    private LocalDateTime departureTime;
    private RideRequestStatus status;          
    private double passengerLat;   
    private double passengerLon;
    private String driverName;     
    private int seatsAvailable;      

    public RideRequestPassengerDto(Long id, Long rideId, String departureAddress, String officeName,
                                   LocalDateTime departureTime, RideRequestStatus status,
                                   double passengerLat, double passengerLon,
                                   String driverName, int seatsAvailable) {
        this.id = id;
        this.rideId = rideId;
        this.departureAddress = departureAddress;
        this.officeName = officeName;
        this.departureTime = departureTime;
        this.status = status;
        this.passengerLat = passengerLat;
        this.passengerLon = passengerLon;
        this.driverName = driverName;
        this.seatsAvailable = seatsAvailable;
    }
}