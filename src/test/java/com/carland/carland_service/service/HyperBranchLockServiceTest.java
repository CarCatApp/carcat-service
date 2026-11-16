package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.VinServiceOpen;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VinServiceOpenRepository;
import com.carland.carland_service.service.commission.CommissionCustomerStatusService;
import com.carland.carland_service.service.commission.CommissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HyperBranchLockServiceTest {

    static final long CAR_ID = 9L;
    static final long USER_ID = 4L;
    static final String VIN = "ABC123";

    @Mock CarRepository carRepository;
    @Mock VinServiceOpenRepository vinServiceOpenRepository;
    @Mock CommissionCustomerStatusService commissionCustomerStatusService;
    @InjectMocks HyperBranchLockService service;

    @BeforeEach
    void carBelongsToUser() {
        Customer owner = Customer.builder().userId(USER_ID).build();
        Car car = Car.builder().carId(CAR_ID).vin(" ab c123 ").customer(owner).build();
        when(carRepository.findByCarId(CAR_ID)).thenReturn(car);
    }

    @Test
    void storedFlagStaysOpen() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.of(VinServiceOpen.builder()
                .vin(VIN)
                .reason(VinServiceOpen.PASSIVE_YEAR)
                .build()));

        assertFalse(service.locked(CAR_ID, USER_ID));

        verify(commissionCustomerStatusService, never()).resolveStatus(" ab c123 ");
    }

    @Test
    void oldLoyaltyLocks() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.empty());
        when(commissionCustomerStatusService.resolveStatus(" ab c123 ")).thenReturn(CommissionStatus.OLD);

        assertTrue(service.locked(CAR_ID, USER_ID));
    }

    @Test
    void newLoyaltyStaysOpen() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.empty());
        when(commissionCustomerStatusService.resolveStatus(" ab c123 ")).thenReturn(CommissionStatus.NEW);

        assertFalse(service.locked(CAR_ID, USER_ID));
    }

    @Test
    void reactivatedLoyaltyStaysOpen() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.empty());
        when(commissionCustomerStatusService.resolveStatus(" ab c123 ")).thenReturn(CommissionStatus.REACTIVATED);

        assertFalse(service.locked(CAR_ID, USER_ID));
    }

    @Test
    void missingCarIsNotFound() {
        when(carRepository.findByCarId(CAR_ID)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.locked(CAR_ID, USER_ID));
    }

    @Test
    void anotherUsersCarIsForbidden() {
        assertThrows(ForbiddenException.class, () -> service.locked(CAR_ID, 99L));
    }
}
