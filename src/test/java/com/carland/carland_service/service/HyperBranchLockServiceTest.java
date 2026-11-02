package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.VinServiceOpen;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ServiceHistoryRepository;
import com.carland.carland_service.repository.VinServiceOpenRepository;
import com.carland.carland_service.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HyperBranchLockServiceTest {

    static final long CAR_ID = 9L;
    static final long USER_ID = 4L;
    static final String VIN = "ABC123";

    @Mock CarRepository carRepository;
    @Mock VisitRepository visitRepository;
    @Mock ServiceHistoryRepository serviceHistoryRepository;
    @Mock BookingRepository bookingRepository;
    @Mock VinServiceOpenRepository vinServiceOpenRepository;
    @Mock VinServiceOpenWriter vinServiceOpenWriter;
    @InjectMocks HyperBranchLockService service;

    @BeforeEach
    void carBelongsToUser() {
        Customer owner = Customer.builder().userId(USER_ID).build();
        Car car = Car.builder().carId(CAR_ID).vin(" ab c123 ").customer(owner).build();
        when(carRepository.findByCarId(CAR_ID)).thenReturn(car);
    }

    @Test
    void vinMissingFromHyperStaysOpen() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.empty());
        when(visitRepository.countHyperVisits(CAR_ID, PartnerId.HYPER.getId())).thenReturn(0L);
        when(serviceHistoryRepository.countHyperRows(CAR_ID, PartnerId.HYPER.getId())).thenReturn(0L);

        assertFalse(service.locked(CAR_ID, USER_ID));

        verify(vinServiceOpenWriter, never()).saveIfAbsent(eq(VIN), eq(VinServiceOpen.PASSIVE_YEAR), eq(CAR_ID));
    }

    @Test
    void storedFlagStaysOpenAfterANewHyperVisit() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.of(VinServiceOpen.builder()
                .vin(VIN)
                .reason(VinServiceOpen.PASSIVE_YEAR)
                .build()));

        assertFalse(service.locked(CAR_ID, USER_ID));

        verify(visitRepository, never()).countHyperVisits(anyLong(), anyLong());
        verify(vinServiceOpenWriter, never()).saveIfAbsent(eq(VIN), eq(VinServiceOpen.PASSIVE_YEAR), eq(CAR_ID));
    }

    @Test
    void recentHyperVisitWithPriorHistoryLocks() {
        hyperVisitOn(LocalDate.now(StaffSlotWindows.ZONE).minusDays(10));
        when(bookingRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(OffsetDateTime.now(StaffSlotWindows.ZONE));
        when(visitRepository.existsByCar_Customer_UserIdAndCreatedAtIsNull(USER_ID)).thenReturn(false);
        when(visitRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(LocalDateTime.now().minusDays(30));

        assertTrue(service.locked(CAR_ID, USER_ID));

        verify(vinServiceOpenWriter, never()).saveIfAbsent(eq(VIN), eq(VinServiceOpen.PASSIVE_YEAR), eq(CAR_ID));
    }

    @Test
    void visitOnTheExactLockDayStaysLocked() {
        hyperVisitOn(LocalDate.now(StaffSlotWindows.ZONE).minusDays(HyperBranchLockService.DEFAULT_LOCK_DAYS));
        when(bookingRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(null);

        assertTrue(service.locked(CAR_ID, USER_ID));

        verify(vinServiceOpenWriter, never()).saveIfAbsent(eq(VIN), eq(VinServiceOpen.PASSIVE_YEAR), eq(CAR_ID));
    }

    @Test
    void visitOlderThanTheLockWritesTheFlag() {
        hyperVisitOn(LocalDate.now(StaffSlotWindows.ZONE).minusDays(HyperBranchLockService.DEFAULT_LOCK_DAYS + 1));

        assertFalse(service.locked(CAR_ID, USER_ID));

        verify(vinServiceOpenWriter).saveIfAbsent(VIN, VinServiceOpen.PASSIVE_YEAR, CAR_ID);
        verify(bookingRepository, never()).findEarliestCreatedAtByCustomer(USER_ID);
    }

    @Test
    void firstBookingWithNoEarlierVisitOpens() {
        hyperVisitOn(LocalDate.now(StaffSlotWindows.ZONE));
        when(bookingRepository.findEarliestCreatedAtByCustomer(USER_ID))
                .thenReturn(OffsetDateTime.now(StaffSlotWindows.ZONE).minusDays(1));
        when(visitRepository.existsByCar_Customer_UserIdAndCreatedAtIsNull(USER_ID)).thenReturn(false);
        when(visitRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(LocalDateTime.now());

        assertFalse(service.locked(CAR_ID, USER_ID));

        verify(vinServiceOpenWriter).saveIfAbsent(VIN, VinServiceOpen.KARKAT_BOOKING, CAR_ID);
    }

    @Test
    void undatedVisitCountsAsHistoryBeforeTheBooking() {
        hyperVisitOn(LocalDate.now(StaffSlotWindows.ZONE));
        when(bookingRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(OffsetDateTime.now(StaffSlotWindows.ZONE));
        when(visitRepository.existsByCar_Customer_UserIdAndCreatedAtIsNull(USER_ID)).thenReturn(true);

        assertTrue(service.locked(CAR_ID, USER_ID));

        verify(vinServiceOpenWriter, never()).saveIfAbsent(eq(VIN), eq(VinServiceOpen.KARKAT_BOOKING), eq(CAR_ID));
    }

    @Test
    void historyWithoutAVisitRowStaysLockedWhenTheCustomerBooked() {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.empty());
        when(visitRepository.countHyperVisits(CAR_ID, PartnerId.HYPER.getId())).thenReturn(0L);
        when(serviceHistoryRepository.countHyperRows(CAR_ID, PartnerId.HYPER.getId())).thenReturn(1L);
        when(visitRepository.findLatestHyperVisitDate(CAR_ID, PartnerId.HYPER.getId())).thenReturn(null);
        when(serviceHistoryRepository.findLatestHyperDoneDate(CAR_ID, PartnerId.HYPER.getId()))
                .thenReturn(LocalDate.now(StaffSlotWindows.ZONE));
        when(bookingRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(OffsetDateTime.now(StaffSlotWindows.ZONE));
        when(visitRepository.existsByCar_Customer_UserIdAndCreatedAtIsNull(USER_ID)).thenReturn(false);
        when(visitRepository.findEarliestCreatedAtByCustomer(USER_ID)).thenReturn(null);
        when(serviceHistoryRepository.existsByCar_Customer_UserId(USER_ID)).thenReturn(true);

        assertTrue(service.locked(CAR_ID, USER_ID));
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

    private void hyperVisitOn(LocalDate day) {
        when(vinServiceOpenRepository.findByVin(VIN)).thenReturn(Optional.empty());
        when(visitRepository.countHyperVisits(CAR_ID, PartnerId.HYPER.getId())).thenReturn(1L);
        when(serviceHistoryRepository.countHyperRows(CAR_ID, PartnerId.HYPER.getId())).thenReturn(0L);
        when(visitRepository.findLatestHyperVisitDate(CAR_ID, PartnerId.HYPER.getId())).thenReturn(day);
        when(serviceHistoryRepository.findLatestHyperDoneDate(CAR_ID, PartnerId.HYPER.getId())).thenReturn(null);
    }
}
