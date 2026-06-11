package kaleidostop.map.car_map.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.domain.Office;

public interface OfficeRepository extends JpaRepository<Office, Long> {
}