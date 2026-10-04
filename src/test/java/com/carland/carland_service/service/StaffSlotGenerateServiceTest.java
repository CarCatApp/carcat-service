package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.StaffSlotDayRule;
import com.carland.carland_service.dto.request.StaffSlotGenerateRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceCategoryRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.RangeRepository;
import com.carland.carland_service.repository.ServiceCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffSlotGenerateServiceTest {

    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock BranchRepository branchRepository;
    @Mock CalendarRepository calendarRepository;
    @Mock RangeRepository rangeRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BranchCarePackageRepository carePackageRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock ServiceCategoryRepository categoryRepository;
    @Mock BranchServiceCategoryRepository branchCategoryRepository;
    @Mock ServiceCategoryJson json;

    StaffSlotGenerateService service;
    Branch branch;
    BookingStaff staff;

    @BeforeEach
    void setUp() {
        service = new StaffSlotGenerateService(
                bookingStaffAccess, branchRepository, calendarRepository, rangeRepository, bookingRepository,
                carePackageRepository, individualServiceRepository, branchIndividualServiceRepository,
                categoryRepository, branchCategoryRepository, json);
        Partner partner = Partner.builder().id(1L).name("HS").build();
        branch = Branch.builder().id(5L).name("Babek").partner(partner).build();
        staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .branch(branch)
                .role(BookingStaffRole.BRANCH_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
    }

    @Test
    void skipsOnlyTheDayThatAlreadyHasThatService() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 5L, "az")).thenReturn(branch);
        BranchCarePackage oil = BranchCarePackage.builder().id(10L).name("Yağ").active(true).branch(branch).price(1).build();
        IndividualService air = IndividualService.builder().id(20L).code("HAF").titleJson("{}").active(true).build();
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(oil));
        when(individualServiceRepository.findById(20L)).thenReturn(Optional.of(air));
        when(branchIndividualServiceRepository.findByBranch_IdAndIndividualService_Id(5L, 20L))
                .thenReturn(Optional.of(BranchIndividualService.builder().active(true).branch(branch).individualService(air).build()));
        when(json.read("{}")).thenReturn(Map.of("az", "Hava filtri"));
        when(rangeRepository.countIndividualOnDay(anyLong(), any(), anyLong())).thenReturn(0L);
        when(rangeRepository.countIndividualOnDay(eq(5L), eq(LocalDate.of(2027, 6, 8)), eq(20L))).thenReturn(1L);
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> {
            Calendar calendar = inv.getArgument(0);
            calendar.setCalendarId(1L);
            return calendar;
        });
        when(rangeRepository.save(any(Range.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.generate(9L, false, request(), "az");

        ArgumentCaptor<Range> saved = ArgumentCaptor.forClass(Range.class);
        verify(rangeRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        LocalDate skipped = LocalDate.of(2027, 6, 8);
        long airOnSkippedDay = saved.getAllValues().stream()
                .filter(range -> StaffSlotTargets.INDIVIDUAL.equals(range.getSlotTarget()))
                .filter(range -> skipped.equals(range.getStart().atZoneSameInstant(StaffSlotWindows.ZONE).toLocalDate()))
                .count();
        long oilOnSkippedDay = saved.getAllValues().stream()
                .filter(range -> StaffSlotTargets.PACKAGE.equals(range.getSlotTarget()))
                .filter(range -> skipped.equals(range.getStart().atZoneSameInstant(StaffSlotWindows.ZONE).toLocalDate()))
                .count();
        assertEquals(0, airOnSkippedDay);
        assertEquals(2, oilOnSkippedDay);
        assertEquals(14, response.getCreated());
        assertTrue(response.getMessage().contains("Hava filtri"));
        assertTrue(response.getMessage().contains("2027-06-08"));
        assertTrue(response.getMessage().contains("Yağ"));
    }

    @Test
    void closedPackageWritesNothing() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 5L, "az")).thenReturn(branch);
        BranchCarePackage oil = BranchCarePackage.builder().id(10L).name("Yağ").active(false).branch(branch).price(1).build();
        when(carePackageRepository.findById(10L)).thenReturn(Optional.of(oil));
        StaffSlotGenerateRequest body = request();
        body.setIndividualServiceIds(List.of());

        ConflictException ex = assertThrows(ConflictException.class, () -> service.generate(9L, false, body, "az"));

        assertTrue(ex.getMessage().contains("Yağ"));
        verify(rangeRepository, never()).save(any());
    }

    private static StaffSlotGenerateRequest request() {
        StaffSlotDayRule weekday = new StaffSlotDayRule();
        weekday.setStart(LocalTime.of(9, 0));
        weekday.setEnd(LocalTime.of(11, 0));
        weekday.setCapacity(2);
        weekday.setBookingMode("approval");
        StaffSlotGenerateRequest body = new StaffSlotGenerateRequest();
        body.setBranchId(5L);
        body.setPackageIds(List.of(10L));
        body.setIndividualServiceIds(List.of(20L));
        body.setRepairInspection(false);
        body.setStartDate(LocalDate.of(2027, 6, 7));
        body.setEndDate(LocalDate.of(2027, 6, 10));
        body.setDurationMin(60);
        body.setSaturday(false);
        body.setSunday(false);
        body.setWeekday(weekday);
        return body;
    }
}
