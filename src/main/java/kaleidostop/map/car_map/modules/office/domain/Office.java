package kaleidostop.map.car_map.modules.office.domain;

import jakarta.persistence.*;
import kaleidostop.map.car_map.modules.office.dto.OfficeRequest;
import kaleidostop.map.car_map.modules.office.dto.OfficeSeed;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "offices")
public class Office {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;
    private Double latitude;
    private Double longitude;


    public static Office fromSeed(OfficeSeed seed) {
        Office office = new Office();
        office.setName(seed.getName());
        office.setAddress(seed.getAddress());
        office.setLatitude(seed.getLatitude());
        office.setLongitude(seed.getLongitude());
        return office;
    }

    public void updateFrom(OfficeRequest request) {
        this.name = request.getName();
        this.address = request.getAddress();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
    }
}