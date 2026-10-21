package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCalendarDayView;
import com.carland.carland_service.dto.booking.BookingCalendarRequest;
import com.carland.carland_service.dto.booking.BookingCalendarResponse;
import com.carland.carland_service.dto.booking.BookingDayRangesRequest;
import com.carland.carland_service.dto.booking.BookingDayRangesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStatus;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

    @Test
    void laterMonthsStayWhenTheBranchStillHasASlot() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        LocalDate later = today.plusMonths(2).withDayOfMonth(15);
        LocalDate end = later.withDayOfMonth(later.lengthOfMonth());
        OffsetDateTime start = ZonedDateTime.of(later, LocalTime.NOON, StaffSlotWindows.ZONE).toOffsetDateTime();
        BranchCarePackage pkg = BranchCarePackage.builder().id(10L).name("Yağ").active(true).branch(branch).price(1).build();
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(pkg));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findLatestDayFrom(7L, today)).thenReturn(later);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(today), eq(end))).thenReturn(List.of(
                day(later, range(2L, start, 2, pkg))
        ));

        BookingCalendarRequest request = new BookingCalendarRequest();
        request.setPackageId(10L);
        BookingCalendarResponse response = service.month(7L, request, "az");

        assertEquals(Boolean.FALSE, available(response, today));
        assertEquals(Boolean.TRUE, available(response, later));
        assertEquals(end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                response.getDays().get(response.getDays().size() - 1).getDate());
        assertEquals((int) ChronoUnit.DAYS.between(today, end) + 1, response.getDays().size());
    }

    @Test
    void sameWindowReturnsTheRangeWithMorePlaces() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        BranchCarePackage pkg = packageRow();
        IndividualService oil = oil();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(40L, start, 2, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(55L, start, 4, StaffSlotTargets.INDIVIDUAL, null, oil),
                        slot(70L, start, 8, StaffSlotTargets.REPAIR_INSPECTION, null, null))
        ));

        BookingDayRangesResponse response = service.day(7L, request(day, 10L, List.of(20L), "səs"), "az");

        assertEquals(1, response.getRanges().size());
        assertEquals(70L, response.getRanges().get(0).getRangeId());
        assertEquals(8, response.getRanges().get(0).getRemaining());
    }

    @Test
    void equalPlacesPreferPackageThenServiceThenRepair() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        BranchCarePackage pkg = packageRow();
        IndividualService oil = oil();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(70L, start, 4, StaffSlotTargets.REPAIR_INSPECTION, null, null),
                        slot(55L, start, 4, StaffSlotTargets.INDIVIDUAL, null, oil),
                        slot(40L, start, 4, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingDayRangesResponse response = service.day(7L, request(day, 10L, List.of(20L), "səs"), "az");

        assertEquals(40L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void tiedServiceBeatsRepairWhenPackageHasFewerPlaces() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        OffsetDateTime later = start.plusHours(2);
        BranchCarePackage pkg = packageRow();
        IndividualService oil = oil();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(40L, start, 2, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(55L, start, 4, StaffSlotTargets.INDIVIDUAL, null, oil),
                        slot(70L, start, 4, StaffSlotTargets.REPAIR_INSPECTION, null, null),
                        wide(61L, later, later.plusHours(1), 2, pkg))
        ));

        BookingDayRangesResponse response = service.day(7L, request(day, 10L, List.of(20L), "səs"), "az");

        assertEquals(2, response.getRanges().size());
        assertEquals(55L, response.getRanges().get(0).getRangeId());
        assertEquals(61L, response.getRanges().get(1).getRangeId());
    }

    @Test
    void sameStartDifferentLengthKeepsTheFullerRangeAndOtherStartsStay() {
        LocalDate day = openDay();
        OffsetDateTime noon = at(day, 12, 0);
        OffsetDateTime halfPast = at(day, 12, 30);
        BranchCarePackage pkg = packageRow();
        IndividualService oil = oil();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        wide(40L, noon, noon.plusMinutes(30), 2, pkg),
                        slot(55L, noon, 5, StaffSlotTargets.INDIVIDUAL, null, oil),
                        wide(80L, noon, noon.plusHours(1), 8, pkg),
                        wide(90L, halfPast, halfPast.plusHours(1), 1, pkg))
        ));

        BookingDayRangesResponse response = service.day(7L, request(day, 10L, List.of(20L), "səs"), "az");

        assertEquals(2, response.getRanges().size());
        assertEquals(80L, response.getRanges().get(0).getRangeId());
        assertEquals("12:00", response.getRanges().get(0).getStart());
        assertEquals("13:00", response.getRanges().get(0).getEnd());
        assertEquals(8, response.getRanges().get(0).getRemaining());
        assertEquals(90L, response.getRanges().get(1).getRangeId());
        assertEquals("12:30", response.getRanges().get(1).getStart());
    }

    @Test
    void blankIssueIgnoresRepairRange() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        BranchCarePackage pkg = packageRow();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(40L, start, 2, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(70L, start, 8, StaffSlotTargets.REPAIR_INSPECTION, null, null))
        ));

        BookingDayRangesResponse response = service.day(7L, request(day, 10L, null, null), "az");

        assertEquals(40L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void pastAndFullRangesAreLeftOut() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        BranchCarePackage pkg = packageRow();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(eq(70L), any())).thenReturn(1L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(1L, OffsetDateTime.now(StaffSlotWindows.ZONE).minusHours(2), 4,
                                StaffSlotTargets.PACKAGE, pkg, null),
                        slot(70L, start, 1, StaffSlotTargets.REPAIR_INSPECTION, null, null),
                        slot(40L, start, 2, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingDayRangesResponse response = service.day(7L, request(day, 10L, null, "səs"), "az");

        assertEquals(1, response.getRanges().size());
        assertEquals(40L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void sameCarHeldRangeIsHiddenAndTheNextHourStays() {
        LocalDate day = openDay();
        BranchCarePackage pkg = packageRow();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findRangeIdsByCarIdAndStatusIn(eq(11L), eq(BookingStatus.holdingCarOnRange())))
                .thenReturn(List.of(40L));
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(40L, at(day, 9, 0), 2, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(41L, at(day, 10, 0), 2, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingDayRangesRequest body = request(day, 10L, null, null);
        body.setCarId(11L);
        BookingDayRangesResponse response = service.day(7L, body, "az");

        assertEquals(1, response.getRanges().size());
        assertEquals(41L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void heldRepairYieldsToAUsablePackageAtTheSameHour() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        OffsetDateTime later = start.plusHours(2);
        BranchCarePackage pkg = packageRow();
        IndividualService oil = oil();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findRangeIdsByCarIdAndStatusIn(eq(11L), eq(BookingStatus.holdingCarOnRange())))
                .thenReturn(List.of(70L));
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(70L, start, 4, StaffSlotTargets.REPAIR_INSPECTION, null, null),
                        slot(40L, start, 2, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(55L, start, 1, StaffSlotTargets.INDIVIDUAL, null, oil),
                        slot(61L, later, 2, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingDayRangesRequest body = request(day, 10L, List.of(20L), "səs");
        body.setCarId(11L);
        BookingDayRangesResponse response = service.day(7L, body, "az");

        assertEquals(2, response.getRanges().size());
        assertEquals(40L, response.getRanges().get(0).getRangeId());
        assertEquals(2, response.getRanges().get(0).getRemaining());
        assertEquals(61L, response.getRanges().get(1).getRangeId());
    }

    @Test
    void singlePackageHidesTheWholeHourWhenASiblingSharesTheStart() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 9, 0);
        OffsetDateTime later = start.plusHours(1);
        BranchCarePackage pkg = packageRow();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findRangeIdsByCarIdAndStatusIn(eq(11L), eq(BookingStatus.holdingCarOnRange())))
                .thenReturn(List.of(40L));
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(40L, start, 4, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(80L, start, 1, StaffSlotTargets.PACKAGE, pkg, null),
                        slot(41L, later, 2, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingDayRangesRequest body = request(day, 10L, null, null);
        body.setCarId(11L);
        BookingDayRangesResponse response = service.day(7L, body, "az");

        assertEquals(1, response.getRanges().size());
        assertEquals(41L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void singleRepairBookingHidesThatHour() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        OffsetDateTime later = start.plusHours(1);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findRangeIdsByCarIdAndStatusIn(eq(11L), eq(BookingStatus.holdingCarOnRange())))
                .thenReturn(List.of(70L));
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(day), eq(day))).thenReturn(List.of(
                day(day,
                        slot(70L, start, 4, StaffSlotTargets.REPAIR_INSPECTION, null, null),
                        slot(71L, later, 4, StaffSlotTargets.REPAIR_INSPECTION, null, null))
        ));

        BookingDayRangesRequest body = request(day, null, null, "səs");
        body.setCarId(11L);
        BookingDayRangesResponse response = service.day(7L, body, "az");

        assertEquals(1, response.getRanges().size());
        assertEquals(71L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void heldRangeDoesNotKeepTheDayOpen() {
        LocalDate day = openDay();
        BranchCarePackage pkg = packageRow();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findRangeIdsByCarIdAndStatusIn(eq(11L), any())).thenReturn(List.of(40L));
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), any(), any())).thenReturn(List.of(
                day(day, slot(40L, at(day, 11, 0), 2, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingCalendarRequest body = new BookingCalendarRequest();
        body.setPackageId(10L);
        body.setCarId(11L);

        assertEquals(Boolean.FALSE, available(service.month(7L, body, "az"), day));
    }

    @Test
    void pastDateIsRejected() {
        LocalDate yesterday = LocalDate.now(StaffSlotWindows.ZONE).minusDays(1);
        assertThrows(MissingFieldException.class,
                () -> service.day(7L, request(yesterday, null, null, "səs"), "az"));
    }

    @Test
    void laterMonthHourIsReturned() {
        LocalDate later = LocalDate.now(StaffSlotWindows.ZONE).plusMonths(1).withDayOfMonth(15);
        BranchCarePackage pkg = packageRow();
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), eq(later), eq(later))).thenReturn(List.of(
                day(later, slot(40L, at(later, 11, 0), 2, StaffSlotTargets.PACKAGE, pkg, null))
        ));

        BookingDayRangesResponse response = service.day(7L, request(later, 10L, null, null), "az");

        assertEquals(1, response.getRanges().size());
        assertEquals(40L, response.getRanges().get(0).getRangeId());
    }

    @Test
    void hiddenDayAndHiddenHourStayOffTheCustomerCalendar() {
        LocalDate day = openDay();
        OffsetDateTime start = at(day, 11, 0);
        BranchCarePackage pkg = packageRow();
        Range closedDay = slot(2L, start, 2, StaffSlotTargets.PACKAGE, pkg, null);
        closedDay.setDayHidden(true);
        Range closedHour = slot(3L, start.plusHours(1), 2, StaffSlotTargets.PACKAGE, pkg, null);
        closedHour.setStatus(RangeStatus.HIDDEN.name());
        Range open = slot(4L, start.plusHours(2), 2, StaffSlotTargets.PACKAGE, pkg, null);
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(calendarRepository.findByBranchIdAndDayBetween(eq(7L), any(), any())).thenReturn(List.of(
                day(day, closedDay, closedHour, open)));

        BookingCalendarRequest monthBody = new BookingCalendarRequest();
        monthBody.setPackageId(10L);
        assertEquals(Boolean.TRUE, available(service.month(7L, monthBody, "az"), day));

        BookingDayRangesResponse ranges = service.day(7L, request(day, 10L, null, null), "az");
        assertEquals(1, ranges.getRanges().size());
        assertEquals(4L, ranges.getRanges().get(0).getRangeId());
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

    private LocalDate openDay() {
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        return today.equals(end) ? today : today.plusDays(1);
    }

    private static OffsetDateTime at(LocalDate day, int hour, int minute) {
        OffsetDateTime start = ZonedDateTime.of(day, LocalTime.of(hour, minute), StaffSlotWindows.ZONE).toOffsetDateTime();
        if (start.isAfter(OffsetDateTime.now(StaffSlotWindows.ZONE))) {
            return start;
        }
        return OffsetDateTime.now(StaffSlotWindows.ZONE).plusMinutes(20);
    }

    private static BookingDayRangesRequest request(LocalDate day, Long packageId, List<Long> services, String issue) {
        BookingDayRangesRequest body = new BookingDayRangesRequest();
        body.setDate(day.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        body.setPackageId(packageId);
        body.setIndividualServiceIds(services);
        body.setIssue(issue);
        return body;
    }

    private BranchCarePackage packageRow() {
        BranchCarePackage pkg = BranchCarePackage.builder().id(10L).name("Yağ").active(true).branch(branch).price(1).build();
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(pkg));
        return pkg;
    }

    private IndividualService oil() {
        IndividualService oil = IndividualService.builder().id(20L).active(true).code("oil").titleJson("{}").build();
        when(individualServiceRepository.findById(20L)).thenReturn(Optional.of(oil));
        when(json.read("{}")).thenReturn(Map.of("az", "Yağ"));
        when(branchIndividualServiceRepository.findByBranch_IdAndIndividualService_Id(7L, 20L))
                .thenReturn(Optional.of(BranchIndividualService.builder()
                        .branch(branch)
                        .individualService(oil)
                        .active(true)
                        .build()));
        return oil;
    }

    private static Range slot(Long id, OffsetDateTime start, int capacity, String target,
                              BranchCarePackage pkg, IndividualService service) {
        return Range.builder()
                .rangeId(id)
                .start(start)
                .end(start.plusMinutes(30))
                .workerCount(capacity)
                .status(RangeStatus.AVAILABLE.name())
                .slotTarget(target)
                .carePackage(pkg)
                .individualService(service)
                .build();
    }

    private static Range wide(Long id, OffsetDateTime start, OffsetDateTime end, int capacity, BranchCarePackage pkg) {
        return Range.builder()
                .rangeId(id)
                .start(start)
                .end(end)
                .workerCount(capacity)
                .status(RangeStatus.AVAILABLE.name())
                .slotTarget(StaffSlotTargets.PACKAGE)
                .carePackage(pkg)
                .build();
    }
}
