package kaleidostop.map.car_map.modules.routing.dto;

import java.util.Map;

public class OsrmRoute {
    private double distance;
    private double duration;
    private Map<String, Object> geometry;

    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
    public double getDuration() {
        return duration;
    }
    public void setDuration(double duration) {
        this.duration = duration;
    }
    public Map<String, Object> getGeometry() {
        return geometry;
    }
    public void setGeometry(Map<String, Object> geometry) {
        this.geometry = geometry;
    }
}
