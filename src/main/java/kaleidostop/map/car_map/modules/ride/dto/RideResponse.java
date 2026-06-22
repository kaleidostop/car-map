package kaleidostop.map.car_map.modules.ride.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class RideResponse {
    private Long id;
    private String driverName;
    private String driverEmail;
    private String officeName;
    private double officeLat;
    private double officeLon;
    private String departureAddress;
    private double departureLat;
    private double departureLon;
    private LocalDateTime departureTime;
    private int seatsTotal;
    private int seatsAvailable;
    private String status;
    private double distanceMeters;
    private double durationSeconds;
    private Map<String, Object> routeGeometry;
    private List<Map<String, Object>> passengers; 

    public RideResponse(Long id, String driverName, String driverEmail, String officeName,
                        double officeLat, double officeLon,
                        String departureAddress, double departureLat, double departureLon,
                        LocalDateTime departureTime, int seatsTotal, int seatsAvailable,
                        String status,
                        double distanceMeters, double durationSeconds,
                        Map<String, Object> routeGeometry, List<Map<String, Object>> passengers) {
        this.id = id;
        this.driverName = driverName;
        this.driverEmail = driverEmail;
        this.officeName = officeName;
        this.officeLat = officeLat;
        this.officeLon = officeLon;
        this.departureAddress = departureAddress;
        this.departureLat = departureLat;
        this.departureLon = departureLon;
        this.departureTime = departureTime;
        this.seatsTotal = seatsTotal;
        this.seatsAvailable = seatsAvailable;
        this.status = status;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.routeGeometry = routeGeometry;
        this.passengers = passengers;
    }
}