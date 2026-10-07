package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.StaffSlotCapacityRequest;
import com.carland.carland_service.dto.request.StaffSlotDayHiddenRequest;
import com.carland.carland_service.dto.request.StaffSlotHiddenRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.RangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffSlotEditServiceTest {

    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock RangeRepository rangeRepository;
    @Mock CalendarRepository calendarRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingCapacityService bookingCapacityService;

    StaffSlotEditService service;
    Branch branch;
    BookingStaff staff;
    Calendar calendar;

    @BeforeEach
    void setUp() {
        service = new StaffSlotEditService(
                bookingStaffAccess, rangeRepository, calendarRepository, bookingRepository, bookingCapacityService);
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
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 5L, "az")).thenReturn(branch);
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
