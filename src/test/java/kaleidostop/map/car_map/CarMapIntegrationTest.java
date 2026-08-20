package kaleidostop.map.car_map;

import kaleidostop.map.car_map.common.security.JwtUtil;
import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;
import kaleidostop.map.car_map.modules.ride.domain.Ride;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideRequestStatus;
import kaleidostop.map.car_map.modules.ride.domain.enums.RideStatus;
import kaleidostop.map.car_map.modules.ride.dto.JoinRideRequest;
import kaleidostop.map.car_map.modules.ride.repository.RideRepository;
import kaleidostop.map.car_map.modules.ride.repository.RideRequestRepository;
import kaleidostop.map.car_map.modules.ride.service.JoinRideService;
import kaleidostop.map.car_map.modules.routing.domain.Route;
import kaleidostop.map.car_map.modules.routing.dto.RouteInfo;
import kaleidostop.map.car_map.modules.routing.repository.RouteRepository;
import kaleidostop.map.car_map.modules.routing.service.RoutingService;
import kaleidostop.map.car_map.modules.user.domain.User;
import kaleidostop.map.car_map.modules.user.domain.enums.Role;
import kaleidostop.map.car_map.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=integration-test-secret-key-that-is-at-least-thirty-two-bytes",
        "jwt.access-token-expiration=60000",
        "app.data-initializer.enabled=false",
        "spring.task.scheduling.enabled=false"
})
class CarMapIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OfficeRepository officeRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private RideRequestRepository rideRequestRepository;
    @Autowired
    private JoinRideService joinRideService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private SimpUserRegistry userRegistry;
    @MockitoBean
    private RoutingService routingService;
    @LocalServerPort
    private int port;

    private User admin;
    private User driver;
    private User passengerOne;
    private User passengerTwo;
    private Office office;
    private Ride oneSeatRide;
    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE ride_requests, rides, routes, offices, users RESTART IDENTITY CASCADE");

        admin = saveUser("admin@example.com", Role.ROLE_ADMIN);
        driver = saveUser("driver@example.com", Role.ROLE_DRIVER);
        passengerOne = saveUser("passenger1@example.com", Role.ROLE_USER);
        passengerTwo = saveUser("passenger2@example.com", Role.ROLE_USER);

        office = new Office();
        office.setName("Main office");
        office.setAddress("Office street, 1");
        office.setLatitude(59.95);
        office.setLongitude(30.31);
        office = officeRepository.save(office);

        Route route = new Route();
        route.setDistanceMeters(1_000.0);
        route.setDurationSeconds(300.0);
        route.setPolyline("{\"type\":\"LineString\",\"coordinates\":[]}");
        route = routeRepository.save(route);

        oneSeatRide = new Ride();
        oneSeatRide.setDriver(driver);
        oneSeatRide.setOffice(office);
        oneSeatRide.setDepartureAddress("Start street, 1");
        oneSeatRide.setDepartureLat(59.90);
        oneSeatRide.setDepartureLon(30.20);
        oneSeatRide.setDepartureTime(LocalDateTime.now().plusHours(1));
        oneSeatRide.setSeatsTotal(1);
        oneSeatRide.setSeatsAvailable(1);
        oneSeatRide.setStatus(RideStatus.ACTIVE);
        oneSeatRide.setManualApproval(false);
        oneSeatRide.setRoute(route);
        oneSeatRide = rideRepository.save(oneSeatRide);

        RouteInfo updatedRoute = new RouteInfo();
        updatedRoute.setDistanceMeters(1_100.0);
        updatedRoute.setDurationSeconds(330.0);
        updatedRoute.setGeometry(Map.of("type", "LineString", "coordinates", List.of()));
        when(routingService.getRouteWithWaypoints(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyList()))
                .thenReturn(updatedRoute);
    }

    @AfterEach
    void stopWebSocketClient() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void jwtAndRolePermissionsAreEnforced() throws Exception {
        mockMvc.perform(get("/api/offices"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/offices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer broken-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/offices").header(HttpHeaders.AUTHORIZATION, bearer(passengerOne)))
                .andExpect(status().isOk());

        String officeJson = """
                {"name":"Second office","address":"Another street, 2",
                 "latitude":59.91,"longitude":30.29}
                """;
        mockMvc.perform(post("/api/offices")
                        .header(HttpHeaders.AUTHORIZATION, bearer(passengerOne))
                        .contentType("application/json")
                        .content(officeJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/offices")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType("application/json")
                        .content(officeJson))
                .andExpect(status().isCreated());

        String rideJson = """
                {"officeId":%d,"departureAddress":"Driver start","departureLat":59.90,
                 "departureLon":30.20,"departureTime":"%s","seatsTotal":2}
                """.formatted(office.getId(), LocalDateTime.now().plusHours(2));
        mockMvc.perform(post("/api/rides")
                        .header(HttpHeaders.AUTHORIZATION, bearer(passengerOne))
                        .contentType("application/json")
                        .content(rideJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rides")
                        .header(HttpHeaders.AUTHORIZATION, bearer(driver))
                        .contentType("application/json")
                        .content(rideJson))
                .andExpect(status().isOk());
    }

    @Test
    void concurrentJoinCannotOverbookLastSeat() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Object> first = executor.submit(() -> joinAfterSignal(passengerOne, 59.91, 30.21, ready, start));
            Future<Object> second = executor.submit(() -> joinAfterSignal(passengerTwo, 59.92, 30.22, ready, start));
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            List<Object> outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter(Map.class::isInstance).count());
            assertEquals(1, outcomes.stream().filter(IllegalStateException.class::isInstance).count());
        }

        Ride storedRide = rideRepository.findById(oneSeatRide.getId()).orElseThrow();
        assertEquals(0, storedRide.getSeatsAvailable());
        assertEquals(RideStatus.FULL, storedRide.getStatus());
        assertEquals(1, rideRequestRepository.countByRideIdAndStatus(
                storedRide.getId(), RideRequestStatus.ACCEPTED));
        assertEquals(1, rideRequestRepository.findAll().size());
    }

    @Test
    void authenticatedWebSocketReceivesUserDestinationMessage() throws Exception {
        StompSession session = connectWebSocket(bearer(passengerOne));
        ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<>(1);
        session.subscribe("/user/queue/request-status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.offer(new String((byte[]) payload));
            }
        });
        assertTrue(awaitUserSubscription(passengerOne.getEmail(), Duration.ofSeconds(5)));

        messagingTemplate.convertAndSendToUser(passengerOne.getEmail(), "/queue/request-status",
                Map.of("status", "ACCEPTED", "message", "accepted"));

        String payload = messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(payload);
        assertTrue(payload.contains("ACCEPTED"));
        session.disconnect();
    }

    @Test
    void websocketRejectsInvalidJwt() {
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> connectWebSocket("Bearer broken-token"));
        assertNotNull(exception.getCause());
    }

    private Object joinAfterSignal(User passenger, double lat, double lon,
                                   CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            JoinRideRequest request = new JoinRideRequest();
            request.setPassengerLat(lat);
            request.setPassengerLon(lon);
            return joinRideService.joinRide(oneSeatRide.getId(), passenger, request);
        } catch (RuntimeException ex) {
            return ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ex;
        }
    }

    private StompSession connectWebSocket(String authorization) throws Exception {
        SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        stompClient = new WebSocketStompClient(sockJsClient);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.set(HttpHeaders.AUTHORIZATION, authorization);
        return stompClient.connectAsync(
                        "http://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
    }

    private boolean awaitUserSubscription(String username, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            SimpUser user = userRegistry.getUser(username);
            if (user != null && user.getSessions().stream()
                    .anyMatch(webSocketSession -> !webSocketSession.getSubscriptions().isEmpty())) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(25);
        }
        return false;
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("unused");
        user.setFullName(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }
}
