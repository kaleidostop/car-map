package kaleidostop.map.car_map.modules.routing.dto;

import java.util.Map;

public class RouteInfo {
    private double distanceMeters;
    private double durationSeconds;
    private Map<String, Object> geometry; 
    
    public double getDistanceMeters() {
        return distanceMeters;
    }
    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
    public double getDurationSeconds() {
        return durationSeconds;
    }
    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    public Map<String, Object> getGeometry() {
        return geometry;
    }
    public void setGeometry(Map<String, Object> geometry) {
        this.geometry = geometry;
    }
}
