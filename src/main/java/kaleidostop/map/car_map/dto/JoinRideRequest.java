package kaleidostop.map.car_map.dto;

import jakarta.validation.constraints.NotNull;

public class JoinRideRequest {
    @NotNull
    private Double passengerLat;
    @NotNull
    private Double passengerLon;
    private String passengerAddress; 
    
    public Double getPassengerLat() {
        return passengerLat;
    }
    public void setPassengerLat(Double passengerLat) {
        this.passengerLat = passengerLat;
    }
    public Double getPassengerLon() {
        return passengerLon;
    }
    public void setPassengerLon(Double passengerLon) {
        this.passengerLon = passengerLon;
    }
    public String getPassengerAddress() {
        return passengerAddress;
    }
    public void setPassengerAddress(String passengerAddress) {
        this.passengerAddress = passengerAddress;
    }
}