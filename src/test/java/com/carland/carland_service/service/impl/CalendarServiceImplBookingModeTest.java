package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.service.BookingStaffAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceImplBookingModeTest {

    @Mock CalendarRepository calendarRepository;
    @Mock BranchRepository branchRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock Helper helper;

    @InjectMocks CalendarServiceImpl service;

    @Test
    void createCalendarPersistsApprovalModeOnRanges() {
        Partner partner = Partner.builder().id(1L).name("HS").build();
        Branch branch = Branch.builder().id(12L).name("Babek").partner(partner).build();
        BookingStaff staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
        when(bookingStaffAccess.requireActive(9L, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 12L, "az")).thenReturn(branch);

        OffsetDateTime start = OffsetDateTime.of(2026, 10, 27, 5, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 10, 27, 5, 30, 0, 0, ZoneOffset.UTC);
        when(helper.getUtcTimeFromDayAndTimeAndTimeZone(any(), eq(LocalTime.of(9, 0)), eq("Asia/Baku")))
                .thenReturn(start);
        when(helper.getUtcTimeFromDayAndTimeAndTimeZone(any(), eq(LocalTime.of(9, 30)), eq("Asia/Baku")))
                .thenReturn(end);
        when(helper.getUtcDayFromUtcTime(start)).thenReturn(LocalDate.of(2026, 10, 27));
        when(helper.getLocalTimeFromUtcUseTZ(any(), eq("Asia/Baku")))
                .thenAnswer(inv -> inv.getArgument(0, OffsetDateTime.class).toLocalTime());
        when(calendarRepository.findByDayAndServiceCategoryAndBranch(any(), any(), eq(branch))).thenReturn(null);
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> {
            Calendar calendar = inv.getArgument(0);
            calendar.setCalendarId(88L);
            return calendar;
        });

        CalendarRequest request = CalendarRequest.builder()
                .day(LocalDate.of(2026, 10, 27))
                .start(LocalTime.of(9, 0))
                .end(LocalTime.of(9, 30))
                .rangeMinutes(30)
                .serviceCategory("*")
                .workerCount(3)
                .branchId(12L)
                .bookingMode("approval")
                .serviceKey("pkg:hyper-extra")
                .build();

        service.createCalendar(request, "+99450", "9", "Asia/Baku", "az");

        ArgumentCaptor<Calendar> captor = ArgumentCaptor.forClass(Calendar.class);
        org.mockito.Mockito.verify(calendarRepository).save(captor.capture());
        List<Range> ranges = captor.getValue().getTimeRanges();
        assertEquals(1, ranges.size());
        assertEquals("approval", ranges.get(0).getBookingMode());
        assertEquals("pkg:hyper-extra", ranges.get(0).getServiceKey());
        assertEquals(3, ranges.get(0).getWorkerCount());
    }

    @Test
    void createCalendarDefaultsInstantAndStar() {
        Partner partner = Partner.builder().id(1L).name("HS").build();
        Branch branch = Branch.builder().id(12L).name("Babek").partner(partner).build();
        BookingStaff staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
        when(bookingStaffAccess.requireActive(9L, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 12L, "az")).thenReturn(branch);
        OffsetDateTime start = OffsetDateTime.of(2026, 10, 27, 5, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 10, 27, 5, 30, 0, 0, ZoneOffset.UTC);
        when(helper.getUtcTimeFromDayAndTimeAndTimeZone(any(), eq(LocalTime.of(9, 0)), eq("Asia/Baku")))
                .thenReturn(start);
        when(helper.getUtcTimeFromDayAndTimeAndTimeZone(any(), eq(LocalTime.of(9, 30)), eq("Asia/Baku")))
                .thenReturn(end);
        when(helper.getUtcDayFromUtcTime(start)).thenReturn(LocalDate.of(2026, 10, 27));
        when(helper.getLocalTimeFromUtcUseTZ(any(), eq("Asia/Baku")))
                .thenAnswer(inv -> inv.getArgument(0, OffsetDateTime.class).toLocalTime());
        when(calendarRepository.findByDayAndServiceCategoryAndBranch(any(), any(), eq(branch))).thenReturn(null);
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> inv.getArgument(0));

        CalendarRequest request = CalendarRequest.builder()
                .day(LocalDate.of(2026, 10, 27))
                .start(LocalTime.of(9, 0))
                .end(LocalTime.of(9, 30))
                .rangeMinutes(30)
                .serviceCategory("*")
                .workerCount(2)
                .branchId(12L)
                .build();

        service.createCalendar(request, "+99450", "9", "Asia/Baku", "az");

        ArgumentCaptor<Calendar> captor = ArgumentCaptor.forClass(Calendar.class);
        org.mockito.Mockito.verify(calendarRepository).save(captor.capture());
        Range range = captor.getValue().getTimeRanges().get(0);
        assertEquals("instant", range.getBookingMode());
        assertEquals("*", range.getServiceKey());
        assertTrue(range.getAppointments() == null || range.getAppointments().isEmpty());
    }
}
