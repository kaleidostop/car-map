package kaleidostop.map.car_map.modules.routing.dto;

import java.util.List;

public class OsrmResponse {
    private List<OsrmRoute> routes;

    public List<OsrmRoute> getRoutes() {
        return routes;
    }
    public void setRoutes(List<OsrmRoute> routes) {
        this.routes = routes;
    }
}