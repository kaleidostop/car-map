package kaleidostop.map.car_map.modules.routing.service;

import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OsrmClientTest {

    @Test
    void returnsNullWhenOsrmReturnsServerError() {
        WebClient client = webClientReturning(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());

        assertNull(new OsrmClient(client).fetchRoute("30.1,59.9;30.2,59.8"));
    }

    @Test
    void returnsNullWhenOsrmConnectionFails() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new IOException("connection reset")))
                .build();

        assertNull(new OsrmClient(client).fetchRoute("30.1,59.9;30.2,59.8"));
    }

    @Test
    void returnsNullWhenOsrmHasNoRoutes() {
        WebClient client = webClientReturning(jsonResponse("{\"routes\":[]}"));

        assertNull(new OsrmClient(client).fetchRoute("30.1,59.9;30.2,59.8"));
    }

    @Test
    void mapsSuccessfulOsrmResponse() {
        WebClient client = webClientReturning(jsonResponse("""
                {"routes":[{"distance":1234.5,"duration":321.0,
                "geometry":{"type":"LineString","coordinates":[]}}]}
                """));

        RouteInfo result = new OsrmClient(client).fetchRoute("30.1,59.9;30.2,59.8");

        assertEquals(1234.5, result.getDistanceMeters());
        assertEquals(321.0, result.getDurationSeconds());
        assertEquals("LineString", result.getGeometry().get("type"));
    }

    private WebClient webClientReturning(ClientResponse response) {
        return WebClient.builder().exchangeFunction(request -> Mono.just(response)).build();
    }

    private ClientResponse jsonResponse(String body) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(body)
                .build();
    }
}
