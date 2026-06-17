package kaleidostop.map.car_map.modules.office.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;

@RestController
@RequestMapping("/api/offices")
public class OfficeController {
    private final OfficeRepository officeRepository;

    public OfficeController(OfficeRepository officeRepository) {
        this.officeRepository = officeRepository;
    }

    @GetMapping
    public ResponseEntity<List<Office>> getAllOffices() {
        return ResponseEntity.ok(officeRepository.findAll());
    }
}