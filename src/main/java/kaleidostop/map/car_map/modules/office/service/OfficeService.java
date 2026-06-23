package kaleidostop.map.car_map.modules.office.service;

import kaleidostop.map.car_map.common.exception.NotFoundException;
import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.dto.OfficeRequest;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .orElseThrow(() -> new NotFoundException("Офис", id));
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
        if (!officeRepository.existsById(id)) {
            throw new NotFoundException("Офис", id);
        }
        officeRepository.deleteById(id);
    }
}
