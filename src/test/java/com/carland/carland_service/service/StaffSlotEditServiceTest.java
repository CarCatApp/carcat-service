package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.StaffSlotCapacityRequest;
import com.carland.carland_service.dto.request.StaffSlotDayHiddenRequest;
import com.carland.carland_service.dto.request.StaffSlotHiddenRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.AppointmentRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.RangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffSlotEditServiceTest {

    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock RangeRepository rangeRepository;
    @Mock CalendarRepository calendarRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingCapacityService bookingCapacityService;
    @Mock AppointmentRepository appointmentRepository;
    @Mock BookingPushService bookingPushService;

    StaffSlotEditService service;
    Branch branch;
    BookingStaff staff;
    Calendar calendar;

    @BeforeEach
    void setUp() {
        service = new StaffSlotEditService(
                bookingStaffAccess, rangeRepository, calendarRepository, bookingRepository, bookingCapacityService,
                appointmentRepository, bookingPushService);
        Partner partner = Partner.builder().id(1L).name("HS").build();
        branch = Branch.builder().id(5L).name("Babek").partner(partner).build();
        staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .branch(branch)
                .role(BookingStaffRole.BRANCH_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
        calendar = Calendar.builder().calendarId(1L).day(LocalDate.of(2026, 10, 7)).branch(branch).build();
        lenient().when(bookingStaffAccess.requireStaff(eq(9L), eq(false), any())).thenReturn(staff);
        lenient().when(bookingStaffAccess.requireWritableBranch(eq(staff), eq(5L), any())).thenReturn(branch);
    }

    @Test
    void capacityStopsAtAcceptedPlacesAndRejectsWaitingWhenFull() {
        Range range = range(8L, 3);
        when(rangeRepository.lockByRangeId(8L)).thenReturn(Optional.of(range));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(8L), any())).thenReturn(2L);

        service.capacity(9L, false, 8L, delta(-1), "az");

        assertEquals(2, range.getWorkerCount());
        verify(bookingCapacityService).closePendingWhenFull(range);

        assertThrows(ConflictException.class, () -> service.capacity(9L, false, 8L, delta(-1), "az"));
        assertEquals(2, range.getWorkerCount());
    }

    @Test
    void emptyHourCannotDropBelowOneOrPassFifty() {
        Range range = range(8L, 1);
        when(rangeRepository.lockByRangeId(8L)).thenReturn(Optional.of(range));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(8L), any())).thenReturn(0L);

        assertThrows(ConflictException.class, () -> service.capacity(9L, false, 8L, delta(-1), "az"));
        assertEquals(1, range.getWorkerCount());
        verify(bookingCapacityService, never()).closePendingWhenFull(any());

        range.setWorkerCount(50);
        assertThrows(ConflictException.class, () -> service.capacity(9L, false, 8L, delta(1), "az"));
        assertEquals(50, range.getWorkerCount());
    }

    @Test
    void hourHideDoesNotClearTheDayFlag() {
        Range range = range(8L, 2);
        range.setDayHidden(true);
        when(rangeRepository.lockByRangeId(8L)).thenReturn(Optional.of(range));

        StaffSlotHiddenRequest hide = new StaffSlotHiddenRequest();
        hide.setHidden(true);
        service.hidden(9L, false, 8L, hide, "az");
        assertEquals(RangeStatus.HIDDEN.name(), range.getStatus());
        assertTrue(range.getDayHidden());

        StaffSlotHiddenRequest open = new StaffSlotHiddenRequest();
        open.setHidden(false);
        service.hidden(9L, false, 8L, open, "az");
        assertEquals(RangeStatus.AVAILABLE.name(), range.getStatus());
        assertTrue(range.getDayHidden());
    }

    @Test
    void dayHideTouchesOnlyTheSelectedPackage() {
        BranchCarePackage oil = BranchCarePackage.builder().id(10L).name("Yağ").build();
        BranchCarePackage air = BranchCarePackage.builder().id(11L).name("Filtr").build();
        Range oilHour = Range.builder()
                .rangeId(1L)
                .slotTarget(StaffSlotTargets.PACKAGE)
                .carePackage(oil)
                .status(RangeStatus.AVAILABLE.name())
                .build();
        Range closedHour = Range.builder()
                .rangeId(2L)
                .slotTarget(StaffSlotTargets.PACKAGE)
                .carePackage(oil)
                .status(RangeStatus.HIDDEN.name())
                .build();
        Range other = Range.builder()
                .rangeId(3L)
                .slotTarget(StaffSlotTargets.PACKAGE)
                .carePackage(air)
                .status(RangeStatus.AVAILABLE.name())
                .build();
        calendar.setTimeRanges(List.of(oilHour, closedHour, other));
        when(calendarRepository.findByBranchIdAndDayBetween(5L, calendar.getDay(), calendar.getDay()))
                .thenReturn(List.of(calendar));

        service.dayHidden(9L, false, day(10L, true), "az");
        assertTrue(oilHour.getDayHidden());
        assertTrue(closedHour.getDayHidden());
        assertEquals(RangeStatus.HIDDEN.name(), closedHour.getStatus());
        assertEquals(null, other.getDayHidden());

        service.dayHidden(9L, false, day(10L, false), "az");
        assertEquals(false, oilHour.getDayHidden());
        assertEquals(false, closedHour.getDayHidden());
        assertEquals(RangeStatus.HIDDEN.name(), closedHour.getStatus());
        assertEquals(RangeStatus.AVAILABLE.name(), other.getStatus());
    }

    @Test
    void deleteTouchesOnlyTheSelectedPackage() {
        BranchCarePackage oil = BranchCarePackage.builder().id(10L).name("Yağ").build();
        Range oilEmpty = target(1L, StaffSlotTargets.PACKAGE, oil);
        Range oilBooked = target(2L, StaffSlotTargets.PACKAGE, oil);
        Range repair = target(3L, StaffSlotTargets.REPAIR_INSPECTION, null);
        dayOf(oilEmpty, oilBooked, repair);
        lock(oilEmpty, oilBooked);
        Booking waiting = Booking.builder().id(31L).range(oilBooked).status(BookingStatus.PENDING.apiValue()).build();
        Booking accepted = Booking.builder().id(32L).range(oilBooked).status(BookingStatus.CONFIRMED.apiValue()).build();
        when(bookingRepository.findByRange_RangeIdInAndStatusIn(List.of(1L, 2L), BookingStatus.holdingCarOnRange()))
                .thenReturn(List.of(waiting, accepted));
        when(bookingRepository.existsByRange_RangeIdIn(List.of(1L))).thenReturn(false);
        when(appointmentRepository.existsByRange_RangeIdIn(List.of(1L))).thenReturn(false);
        when(bookingRepository.existsByRange_RangeIdIn(List.of(2L))).thenReturn(true);

        service.deleteCalendar(9L, false, 1L, 10L, null, null, "az");

        for (Booking booking : List.of(waiting, accepted)) {
            assertEquals(BookingStatus.REJECTED.apiValue(), booking.getStatus());
            assertEquals(BookingSystemNotes.CALENDAR_DELETED, booking.getCancelReasonCode());
            assertEquals("Xidmət təqvimi silindi", booking.getCancelNote());
        }
        verify(bookingPushService, times(2)).rejected(any());
        assertEquals(RangeStatus.DELETED.name(), oilBooked.getStatus());
        assertEquals(List.of(oilBooked, repair), calendar.getTimeRanges());
        assertEquals(RangeStatus.AVAILABLE.name(), repair.getStatus());
        verify(rangeRepository, never()).lockByRangeId(3L);
        verify(calendarRepository, never()).delete(any());
    }

    @Test
    void lastServiceOfTheDayRemovesTheCalendar() {
        Range repair = target(3L, StaffSlotTargets.REPAIR_INSPECTION, null);
        dayOf(repair);
        lock(repair);
        when(bookingRepository.findByRange_RangeIdInAndStatusIn(List.of(3L), BookingStatus.holdingCarOnRange()))
                .thenReturn(List.of());
        when(bookingRepository.existsByRange_RangeIdIn(List.of(3L))).thenReturn(false);
        when(appointmentRepository.existsByRange_RangeIdIn(List.of(3L))).thenReturn(false);

        service.deleteCalendar(9L, false, 1L, null, null, true, "az");

        verify(calendarRepository).delete(calendar);
        verify(bookingPushService, never()).rejected(any());
    }

    @Test
    void deleteNeedsExactlyOneService() {
        assertThrows(MissingFieldException.class,
                () -> service.deleteCalendar(9L, false, 1L, null, null, null, "az"));
        assertThrows(MissingFieldException.class,
                () -> service.deleteCalendar(9L, false, 1L, 10L, 20L, null, "az"));
        verify(calendarRepository, never()).delete(any());
    }

    @Test
    void serviceWithoutHoursThatDayIsNotFound() {
        dayOf(target(3L, StaffSlotTargets.REPAIR_INSPECTION, null));

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteCalendar(9L, false, 1L, 10L, null, null, "az"));
        verify(calendarRepository, never()).delete(any());
    }

    @Test
    void deleteFromTodayClearsOnePackageOnEveryRemainingDay() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        BranchCarePackage oil = BranchCarePackage.builder().id(10L).name("Yağ").build();
        BranchCarePackage air = BranchCarePackage.builder().id(11L).name("Filtr").build();
        IndividualService brake = IndividualService.builder().id(20L).build();

        Range pastOil = target(11L, StaffSlotTargets.PACKAGE, oil);
        Calendar past = calendarOn(2L, today.minusDays(1), pastOil);

        Range hiddenOil = target(12L, StaffSlotTargets.PACKAGE, oil);
        hiddenOil.setStatus(RangeStatus.HIDDEN.name());
        Range repair = target(13L, StaffSlotTargets.REPAIR_INSPECTION, null);
        Calendar todayCal = calendarOn(3L, today, hiddenOil, repair);

        Range booked = target(14L, StaffSlotTargets.PACKAGE, oil);
        Range emptyOil = target(15L, StaffSlotTargets.PACKAGE, oil);
        Range otherPackage = target(16L, StaffSlotTargets.PACKAGE, air);
        Range individual = target(18L, StaffSlotTargets.INDIVIDUAL, null);
        individual.setIndividualService(brake);
        Calendar later = calendarOn(4L, today.plusDays(3), booked, emptyOil, otherPackage, individual);

        Range onlyOil = target(17L, StaffSlotTargets.PACKAGE, oil);
        Calendar last = calendarOn(5L, today.plusDays(10), onlyOil);

        when(calendarRepository.findLatestDayFrom(5L, today)).thenReturn(last.getDay());
        when(calendarRepository.findByBranchIdAndDayBetween(5L, today, last.getDay()))
                .thenReturn(List.of(past, todayCal, later, last));
        lock(hiddenOil, booked, emptyOil, onlyOil);
        Booking waiting = Booking.builder().id(31L).range(booked).status(BookingStatus.PENDING.apiValue()).build();
        Booking accepted = Booking.builder().id(32L).range(booked).status(BookingStatus.AUTO_ACCEPTED.apiValue()).build();
        when(bookingRepository.findByRange_RangeIdInAndStatusIn(
                List.of(12L, 14L, 15L, 17L), BookingStatus.holdingCarOnRange()))
                .thenReturn(List.of(waiting, accepted));
        lenient().when(bookingRepository.existsByRange_RangeIdIn(List.of(14L))).thenReturn(true);

        String message = service.deleteFromToday(9L, false, 5L, 10L, null, null, "az");

        assertEquals("Xidmətin slotları silindi, rezervasiyalar rədd edildi", message);
        for (Booking booking : List.of(waiting, accepted)) {
            assertEquals(BookingStatus.REJECTED.apiValue(), booking.getStatus());
            assertEquals(BookingSystemNotes.CALENDAR_DELETED, booking.getCancelReasonCode());
            assertEquals("Xidmət təqvimi silindi", booking.getCancelNote());
        }
        verify(bookingPushService, times(2)).rejected(any());
        assertEquals(List.of(pastOil), past.getTimeRanges());
        assertEquals(RangeStatus.AVAILABLE.name(), pastOil.getStatus());
        assertEquals(List.of(repair), todayCal.getTimeRanges());
        assertEquals(RangeStatus.AVAILABLE.name(), repair.getStatus());
        assertEquals(RangeStatus.DELETED.name(), booked.getStatus());
        assertEquals(List.of(booked, otherPackage, individual), later.getTimeRanges());
        assertEquals(RangeStatus.AVAILABLE.name(), otherPackage.getStatus());
        assertEquals(RangeStatus.AVAILABLE.name(), individual.getStatus());
        verify(calendarRepository).delete(last);
        verify(calendarRepository, never()).delete(past);
        verify(calendarRepository, never()).delete(todayCal);
        verify(calendarRepository, never()).delete(later);
        verify(rangeRepository, never()).lockByRangeId(11L);
        verify(rangeRepository, never()).lockByRangeId(13L);
        verify(rangeRepository, never()).lockByRangeId(16L);
        verify(rangeRepository, never()).lockByRangeId(18L);
    }

    @Test
    void deleteFromTodayDropsRepairHoursAndAnEmptyCalendar() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        BranchCarePackage oil = BranchCarePackage.builder().id(10L).name("Yağ").build();
        Range repairToday = target(21L, StaffSlotTargets.REPAIR_INSPECTION, null);
        Range packageToday = target(22L, StaffSlotTargets.PACKAGE, oil);
        Calendar todayCal = calendarOn(6L, today, repairToday, packageToday);
        Range repairLater = target(23L, StaffSlotTargets.REPAIR_INSPECTION, null);
        Calendar later = calendarOn(7L, today.plusDays(1), repairLater);
        when(calendarRepository.findLatestDayFrom(5L, today)).thenReturn(later.getDay());
        when(calendarRepository.findByBranchIdAndDayBetween(5L, today, later.getDay()))
                .thenReturn(List.of(todayCal, later));
        lock(repairToday, repairLater);
        when(bookingRepository.findByRange_RangeIdInAndStatusIn(
                List.of(21L, 23L), BookingStatus.holdingCarOnRange())).thenReturn(List.of());

        String message = service.deleteFromToday(9L, false, null, null, null, true, "az");

        assertEquals("Xidmətin slotları silindi", message);
        assertEquals(List.of(packageToday), todayCal.getTimeRanges());
        assertEquals(RangeStatus.AVAILABLE.name(), packageToday.getStatus());
        verify(calendarRepository).delete(later);
        verify(calendarRepository, never()).delete(todayCal);
        verify(bookingPushService, never()).rejected(any());
        verify(rangeRepository, never()).lockByRangeId(22L);
    }

    @Test
    void deleteFromTodayClearsOneIndividualService() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        IndividualService brake = IndividualService.builder().id(20L).build();
        IndividualService other = IndividualService.builder().id(21L).build();
        Range brakeHour = target(31L, StaffSlotTargets.INDIVIDUAL, null);
        brakeHour.setIndividualService(brake);
        Range otherHour = target(32L, StaffSlotTargets.INDIVIDUAL, null);
        otherHour.setIndividualService(other);
        Calendar todayCal = calendarOn(8L, today, brakeHour, otherHour);
        when(calendarRepository.findLatestDayFrom(5L, today)).thenReturn(today);
        when(calendarRepository.findByBranchIdAndDayBetween(5L, today, today)).thenReturn(List.of(todayCal));
        lock(brakeHour);
        when(bookingRepository.findByRange_RangeIdInAndStatusIn(
                List.of(31L), BookingStatus.holdingCarOnRange())).thenReturn(List.of());
        when(appointmentRepository.existsByRange_RangeIdIn(List.of(31L))).thenReturn(true);

        String message = service.deleteFromToday(9L, false, 5L, null, 20L, null, "en");

        assertEquals("The service slots were deleted", message);
        assertEquals(RangeStatus.DELETED.name(), brakeHour.getStatus());
        assertEquals(List.of(brakeHour, otherHour), todayCal.getTimeRanges());
        assertEquals(RangeStatus.AVAILABLE.name(), otherHour.getStatus());
        verify(calendarRepository, never()).delete(any());
    }

    @Test
    void deleteFromTodayNeedsExactlyOneService() {
        assertThrows(MissingFieldException.class,
                () -> service.deleteFromToday(9L, false, 5L, null, null, null, "az"));
        assertThrows(MissingFieldException.class,
                () -> service.deleteFromToday(9L, false, 5L, 10L, 20L, null, "az"));
        verify(calendarRepository, never()).findLatestDayFrom(any(), any());
    }

    @Test
    void deleteFromTodayWithoutThisServiceIsNotFound() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        Range repair = target(41L, StaffSlotTargets.REPAIR_INSPECTION, null);
        Calendar todayCal = calendarOn(9L, today, repair);
        when(calendarRepository.findLatestDayFrom(eq(5L), any(LocalDate.class))).thenReturn(today);
        when(calendarRepository.findByBranchIdAndDayBetween(5L, today, today)).thenReturn(List.of(todayCal));

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteFromToday(9L, false, 5L, 10L, null, null, "az"));
        assertEquals(List.of(repair), todayCal.getTimeRanges());
        verify(calendarRepository, never()).delete(any());
        verify(rangeRepository, never()).lockByRangeId(any());
    }

    @Test
    void deleteFromTodayWithoutAnyFutureDayIsNotFound() {
        when(calendarRepository.findLatestDayFrom(eq(5L), any(LocalDate.class))).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteFromToday(9L, false, 5L, null, 20L, null, "az"));
        verify(calendarRepository, never()).findByBranchIdAndDayBetween(any(), any(), any());
        verify(rangeRepository, never()).lockByRangeId(any());
    }

    @Test
    void deletedHourCannotBeEdited() {
        Range range = range(8L, 2);
        range.setStatus(RangeStatus.DELETED.name());
        when(rangeRepository.lockByRangeId(8L)).thenReturn(Optional.of(range));

        assertThrows(ResourceNotFoundException.class, () -> service.capacity(9L, false, 8L, delta(1), "az"));
    }

    private Range target(Long id, String slotTarget, BranchCarePackage carePackage) {
        Range range = range(id, 2);
        range.setSlotTarget(slotTarget);
        range.setCarePackage(carePackage);
        return range;
    }

    private void dayOf(Range... ranges) {
        calendar.setTimeRanges(new ArrayList<>(List.of(ranges)));
        when(calendarRepository.findById(1L)).thenReturn(Optional.of(calendar));
    }

    private Calendar calendarOn(Long id, LocalDate day, Range... ranges) {
        Calendar row = Calendar.builder()
                .calendarId(id)
                .day(day)
                .branch(branch)
                .timeRanges(new ArrayList<>(List.of(ranges)))
                .build();
        for (Range range : ranges) {
            range.setCalendar(row);
        }
        return row;
    }

    private void lock(Range... ranges) {
        for (Range range : ranges) {
            when(rangeRepository.lockByRangeId(range.getRangeId())).thenReturn(Optional.of(range));
        }
    }

    private Range range(Long id, int capacity) {
        return Range.builder()
                .rangeId(id)
                .workerCount(capacity)
                .status(RangeStatus.AVAILABLE.name())
                .calendar(calendar)
                .build();
    }

    private static StaffSlotCapacityRequest delta(int value) {
        StaffSlotCapacityRequest body = new StaffSlotCapacityRequest();
        body.setDelta(value);
        return body;
    }

    private StaffSlotDayHiddenRequest day(Long packageId, boolean hidden) {
        StaffSlotDayHiddenRequest body = new StaffSlotDayHiddenRequest();
        body.setBranchId(branch.getId());
        body.setDay(calendar.getDay().toString());
        body.setPackageId(packageId);
        body.setHidden(hidden);
        return body;
    }
}
