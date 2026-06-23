package kaleidostop.map.car_map.modules.office.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kaleidostop.map.car_map.modules.office.domain.Office;
import kaleidostop.map.car_map.modules.office.dto.OfficeRequest;
import kaleidostop.map.car_map.modules.office.repository.OfficeRepository;
import kaleidostop.map.car_map.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

    @Mock
    private OfficeRepository officeRepository;

    @InjectMocks
    private OfficeService officeService;

    private OfficeRequest validRequest;
    private Office savedOffice;

    @BeforeEach
    void setUp() {
        validRequest = new OfficeRequest();
        validRequest.setName("Главный корпус");
        validRequest.setAddress("Кронверкский пр., 49");
        validRequest.setLatitude(59.956);
        validRequest.setLongitude(30.310);

        savedOffice = new Office();
        savedOffice.setId(1L);
        savedOffice.setName("Главный корпус");
        savedOffice.setAddress("Кронверкский пр., 49");
        savedOffice.setLatitude(59.956);
        savedOffice.setLongitude(30.310);
    }

    @Test
    void create_ShouldReturnSavedOffice() {
        when(officeRepository.save(any(Office.class))).thenReturn(savedOffice);

        Office result = officeService.create(validRequest);

        assertEquals(savedOffice.getId(), result.getId());
        assertEquals(validRequest.getName(), result.getName());
        verify(officeRepository).save(any(Office.class));
    }

    @Test
    void getById_WhenExists_ShouldReturnOffice() {
        when(officeRepository.findById(1L)).thenReturn(Optional.of(savedOffice));

        Office result = officeService.getById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getById_WhenNotFound_ShouldThrowOfficeNotFoundException() {
        when(officeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> officeService.getById(99L));
    }

    @Test
    void update_WhenExists_ShouldUpdateAndReturn() {
        when(officeRepository.findById(1L)).thenReturn(Optional.of(savedOffice));
        when(officeRepository.save(any(Office.class))).thenReturn(savedOffice);

        Office result = officeService.update(1L, validRequest);

        assertEquals(validRequest.getName(), result.getName());
        verify(officeRepository).save(any(Office.class));
    }

    @Test
    void update_WhenNotFound_ShouldThrowOfficeNotFoundException() {
        when(officeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> officeService.update(99L, validRequest));
    }

    @Test
    void delete_WhenExists_ShouldSucceed() {
        when(officeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(officeRepository).deleteById(1L);

        assertDoesNotThrow(() -> officeService.delete(1L));
        verify(officeRepository).deleteById(1L);
    }

    @Test
    void delete_WhenNotFound_ShouldThrowOfficeNotFoundException() {
        when(officeRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> officeService.delete(99L));
        verify(officeRepository, never()).deleteById(any());
    }
}