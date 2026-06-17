package kaleidostop.map.car_map.modules.ride.dto;

public class RideRequestDto {
    private Long id;
    private String passengerName;
    private double passengerLat;
    private double passengerLon;

    public RideRequestDto(Long id, String passengerName, double passengerLat, double passengerLon) {
        this.id = id;
        this.passengerName = passengerName;
        this.passengerLat = passengerLat;
        this.passengerLon = passengerLon;
    }
    
    public Long getId() {
        return id;
    }
    public String getPassengerName() {
        return passengerName;
    }
    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }
    public double getPassengerLat() {
        return passengerLat;
    }
    public void setPassengerLat(double passengerLat) {
        this.passengerLat = passengerLat;
    }
    public double getPassengerLon() {
        return passengerLon;
    }
    public void setPassengerLon(double passengerLon) {
        this.passengerLon = passengerLon;
    }
}