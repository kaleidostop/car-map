package kaleidostop.map.car_map.modules.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
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
}