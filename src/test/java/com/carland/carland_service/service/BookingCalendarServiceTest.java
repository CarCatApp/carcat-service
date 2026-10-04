package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCalendarDayView;
import com.carland.carland_service.dto.booking.BookingCalendarRequest;
import com.carland.carland_service.dto.booking.BookingCalendarResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCalendarServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock CalendarRepository calendarRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BranchCarePackageRepository carePackageRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock ServiceCategoryJson json;

    BookingCalendarService service;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingCalendarService(
                branchRepository, calendarRepository, bookingRepository, carePackageRepository,
                individualServiceRepository, branchIndividualServiceRepository, json);
        Partner partner = Partner.builder().id(1L).name("HS").active(true).build();
        branch = Branch.builder().id(7L).name("Babek").active(true).partner(partner).build();
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
    }

    @Test
    void dayIsAvailableOnlyWhenAFuturePlaceRemains() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        LocalDate openDay = today.equals(today.withDayOfMonth(today.lengthOfMonth())) ? today : today.plusDays(1);
        OffsetDateTime openStart = openDay.equals(today)
                ? OffsetDateTime.now(StaffSlotWindows.ZONE).plusMinutes(20)
                : ZonedDateTime.of(openDay, LocalTime.NOON, StaffSlotWindows.ZONE).toOffsetDateTime();
        BranchCarePackage pkg = BranchCarePackage.builder().id(10L).name("Yağ").active(true).branch(branch).price(1).build();
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(pkg));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(3L), any())).thenReturn(1L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), any(), any())).thenReturn(List.of(
                day(today, range(1L, OffsetDateTime.now(StaffSlotWindows.ZONE).minusHours(2), 2, pkg)),
                day(openDay,
                        range(3L, openStart.plusHours(1), 1, pkg),
                        range(2L, openStart, 2, pkg))
        ));

        BookingCalendarRequest request = new BookingCalendarRequest();
        request.setPackageId(10L);
        BookingCalendarResponse response = service.month(7L, request, "az");

        if (openDay.equals(today)) {
            assertEquals(Boolean.TRUE, available(response, today));
            return;
        }
        assertEquals(Boolean.FALSE, available(response, today));
        assertEquals(Boolean.TRUE, available(response, openDay));
    }

    @Test
    void blankIssueIgnoresRepairAndTextUsesIt() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        OffsetDateTime start = OffsetDateTime.now(StaffSlotWindows.ZONE).plusMinutes(30);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), any(), any())).thenReturn(List.of(
                day(today, repair(4L, start))
        ));

        BookingCalendarRequest blank = new BookingCalendarRequest();
        blank.setIssue("  ");
        assertThrows(MissingFieldException.class, () -> service.month(7L, blank, "az"));

        BookingCalendarRequest issue = new BookingCalendarRequest();
        issue.setIssue("kənardan səs gəlir");
        BookingCalendarResponse response = service.month(7L, issue, "az");
        assertEquals(Boolean.TRUE, available(response, today));
        assertEquals(today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), response.getDays().get(0).getDate());
    }

    private static Boolean available(BookingCalendarResponse response, LocalDate day) {
        String formatted = day.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return response.getDays().stream()
                .filter(row -> formatted.equals(row.getDate()))
                .map(BookingCalendarDayView::getAvailable)
                .findFirst()
                .orElseThrow();
    }

    private Calendar day(LocalDate date, Range... ranges) {
        return Calendar.builder()
                .day(date)
                .branch(branch)
                .serviceCategory(StaffSlotTargets.CALENDAR_CATEGORY)
                .timeRanges(List.of(ranges))
                .build();
    }

    private static Range range(Long id, OffsetDateTime start, int capacity, BranchCarePackage pkg) {
        return Range.builder()
                .rangeId(id)
                .start(start)
                .end(start.plusMinutes(30))
                .workerCount(capacity)
                .status(RangeStatus.AVAILABLE.name())
                .slotTarget(StaffSlotTargets.PACKAGE)
                .carePackage(pkg)
                .build();
    }

    private static Range repair(Long id, OffsetDateTime start) {
        return Range.builder()
                .rangeId(id)
                .start(start)
                .end(start.plusMinutes(30))
                .workerCount(2)
                .status(RangeStatus.AVAILABLE.name())
                .slotTarget(StaffSlotTargets.REPAIR_INSPECTION)
                .build();
    }
}
