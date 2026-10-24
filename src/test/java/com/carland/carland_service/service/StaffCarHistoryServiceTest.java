package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffCarHistoryResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.CarRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffCarHistoryServiceTest {

    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock CarRepository carRepository;
    @Mock VisitHistoryService visitHistoryService;
    @InjectMocks StaffCarHistoryService service;

    @Test
    void keepsVisitsThatBelongToTheRequestedBranch() {
        BookingStaff staff = BookingStaff.builder().id(1L).build();
        Branch branch = Branch.builder().id(10L).name("Hyper Service").build();
        Car car = Car.builder().carId(5L).vin("VIN1").brand("BMW").model("X5")
                .plateNumber("77AB").modelYear(2020).build();
        when(bookingStaffAccess.requireStaff(1L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 10L, "az")).thenReturn(branch);
        when(carRepository.findByCarId(5L)).thenReturn(car);
        VisitHistoryItemResponse atBranch = VisitHistoryItemResponse.builder()
                .id(2L).branchId(10L).date(LocalDate.of(2026, 3, 1)).build();
        VisitHistoryItemResponse otherBranch = VisitHistoryItemResponse.builder()
                .id(9L).branchId(22L).date(LocalDate.of(2026, 4, 1)).build();
        when(visitHistoryService.listStoredByCar(car, "az")).thenReturn(List.of(otherBranch, atBranch));

        StaffCarHistoryResponse response = service.history(1L, false, 5L, "VIN1", 10L, "az");

        assertEquals(1, response.getItems().size());
        assertEquals(2L, response.getItems().get(0).getId());
        assertEquals(10L, response.getBranchId());
        assertEquals("Hyper Service", response.getBranchName());
        assertEquals("VIN1", response.getVin());
    }

    @Test
    void unknownCarReturnsAnEmptyBranchHistory() {
        BookingStaff staff = BookingStaff.builder().id(1L).build();
        Branch branch = Branch.builder().id(22L).name("Xırdalan").build();
        when(bookingStaffAccess.requireStaff(1L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 22L, "az")).thenReturn(branch);
        when(carRepository.findByCarId(9L)).thenReturn(null);
        when(carRepository.findAllByVinIgnoreCase("NOPE")).thenReturn(List.of());

        StaffCarHistoryResponse response = service.history(1L, false, 9L, "NOPE", 22L, "az");

        assertTrue(response.getItems().isEmpty());
        assertEquals("NOPE", response.getVin());
        verify(visitHistoryService, never()).listStoredByCar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingVinAndCarIdIsRejected() {
        BookingStaff staff = BookingStaff.builder().id(1L).build();
        Branch branch = Branch.builder().id(10L).name("Hyper Service").build();
        when(bookingStaffAccess.requireStaff(1L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 10L, "az")).thenReturn(branch);

        assertThrows(MissingFieldException.class, () -> service.history(1L, false, null, "  ", 10L, "az"));
    }
}
