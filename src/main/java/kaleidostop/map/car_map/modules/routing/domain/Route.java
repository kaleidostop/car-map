package kaleidostop.map.car_map.modules.routing.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String polyline;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}