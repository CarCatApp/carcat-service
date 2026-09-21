package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingAvailabilityResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingAvailabilityServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock CalendarRepository calendarRepository;
    @Mock BookingRepository bookingRepository;

    BookingAvailabilityService service;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingAvailabilityService(branchRepository, calendarRepository, bookingRepository);
        Partner hyper = Partner.builder().id(1L).name("Hyper").active(true).source("hyper").build();
        branch = Branch.builder().id(7L).name("Xeqani").active(true).partner(hyper).build();
    }

    @Test
    void starSlotMatchesPackageRequest() {
        assertTrue(BookingAvailabilityService.matchesKey("*", List.of("pkg:hyper-extra")));
        assertTrue(BookingAvailabilityService.matchesKey("pkg:hyper-extra", List.of("pkg:hyper-extra")));
        assertFalse(BookingAvailabilityService.matchesKey("pkg:hyper-extra", List.of("svc:oil-change")));
    }

    @Test
    void dayReturnsApprovalSlots() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(calendarRepository.findByBranchIdAndDayBetween(7L, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 26)))
                .thenReturn(List.of(calendar(8L, LocalDate.of(2026, 10, 26), "approval", "pkg:hyper-extra", 105)));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);

        BookingAvailabilityResponse out = service.availability(
                7L, "pkg:hyper-extra", "2026-10-26", "2026-10-26", "Asia/Baku");

        assertEquals("2026-10-26", out.getDay());
        assertEquals(1, out.getRanges().size());
        assertEquals(105L, out.getRanges().get(0).getSlotId());
        assertEquals("09:00", out.getRanges().get(0).getStart());
        assertEquals("approval", out.getRanges().get(0).getBookingMode());
        assertTrue(out.getRanges().get(0).getBookable());
        assertEquals(100, out.getRemainingPercent());
    }

    @Test
    void monthListsDaysWithSlots() {
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(calendarRepository.findByBranchIdAndDayBetween(7L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(
                        calendar(8L, LocalDate.of(2026, 10, 26), "approval", "pkg:hyper-extra", 105),
                        calendar(7L, LocalDate.of(2026, 10, 27), "instant", "*", 87)
                ));
        when(bookingRepository.countByRange_RangeIdAndStatusIn(anyLong(), any())).thenReturn(0L);

        BookingAvailabilityResponse out = service.availability(
                7L, "pkg:hyper-extra", "2026-10-01", "2026-10-31", "Asia/Baku");

        assertEquals(2, out.getDays().size());
        assertEquals("2026-10-26", out.getDays().get(0).getDay());
        assertEquals("available", out.getDays().get(0).getState());
    }

    private Calendar calendar(Long id, LocalDate day, String mode, String key, long rangeId) {
        Range range = Range.builder()
                .rangeId(rangeId)
                .start(OffsetDateTime.parse("2026-10-26T05:00:00Z"))
                .end(OffsetDateTime.parse("2026-10-26T05:30:00Z"))
                .status("AVAILABLE")
                .workerCount(3)
                .bookingMode(mode)
                .serviceKey(key)
                .appointments(new ArrayList<>())
                .build();
        Calendar calendar = Calendar.builder()
                .calendarId(id)
                .day(day)
                .branch(branch)
                .timeRanges(new ArrayList<>(List.of(range)))
                .build();
        range.setCalendar(calendar);
        return calendar;
    }
}
