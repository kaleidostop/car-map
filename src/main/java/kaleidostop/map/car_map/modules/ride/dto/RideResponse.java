package kaleidostop.map.car_map.modules.ride.dto;

import kaleidostop.map.car_map.modules.ride.domain.Ride;
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

    public static RideResponse from(Ride ride, double distance, double duration,
                                    Map<String, Object> geometry,
                                    List<Map<String, Object>> passengers) {
        return new RideResponse(
                ride.getId(), ride.getDriver().getFullName(), ride.getDriver().getEmail(),
                ride.getOffice().getName(), ride.getOffice().getLatitude(), ride.getOffice().getLongitude(),
                ride.getDepartureAddress(), ride.getDepartureLat(), ride.getDepartureLon(),
                ride.getDepartureTime(), ride.getSeatsTotal(), ride.getSeatsAvailable(),
                ride.getStatus().name(), distance, duration, geometry, passengers
        );
    }
}