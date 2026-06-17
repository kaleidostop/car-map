package kaleidostop.map.car_map.modules.office.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kaleidostop.map.car_map.modules.office.domain.Office;

public interface OfficeRepository extends JpaRepository<Office, Long> {
}