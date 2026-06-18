package kaleidostop.map.car_map.modules.office.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.dto.OfficeRequest;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;

@Service
public class OfficeService {
    private final OfficeRepository officeRepository;

    public OfficeService(OfficeRepository officeRepository) {
        this.officeRepository = officeRepository;
    }

    public List<Office> getAll() {
        return officeRepository.findAll();
    }

    public Office getById(Long id) {
        return officeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Офис не найден"));
    }

    @Transactional
    public Office create(OfficeRequest request) {
        Office office = new Office();
        office.setName(request.getName());
        office.setAddress(request.getAddress());
        office.setLatitude(request.getLatitude());
        office.setLongitude(request.getLongitude());
        return officeRepository.save(office);
    }

    @Transactional
    public Office update(Long id, OfficeRequest request) {
        Office office = getById(id);
        office.setName(request.getName());
        office.setAddress(request.getAddress());
        office.setLatitude(request.getLatitude());
        office.setLongitude(request.getLongitude());
        return officeRepository.save(office);
    }

    @Transactional
    public void delete(Long id) {
        officeRepository.deleteById(id);
    }
}
