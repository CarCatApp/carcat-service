package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBranchProfileResponse;
import com.carland.carland_service.dto.booking.BookingBranchProfileServiceView;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchServiceCategory;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.ServiceCategory;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceCategoryRepository;
import com.carland.carland_service.repository.PartnerPhotoRepository;
import com.carland.carland_service.repository.ServiceCategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingBranchProfileServiceTest {

    @Mock BranchRepository branchRepository;
    @Mock PartnerPhotoRepository partnerPhotoRepository;
    @Mock ServiceCategoryRepository categoryRepository;
    @Mock BranchServiceCategoryRepository branchCategoryRepository;
    @Mock BranchCarePackageRepository carePackageRepository;

    BookingBranchProfileService service;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new BookingBranchProfileService(
                branchRepository, partnerPhotoRepository, categoryRepository, branchCategoryRepository,
                carePackageRepository, new ServiceCategoryJson(new ObjectMapper()));
        Partner hyper = Partner.builder().id(1L).name("Hyper").active(true).build();
        branch = Branch.builder()
                .id(7L)
                .name("Xəqani Express")
                .address("43A Babek")
                .lat(40.379)
                .lng(49.846)
                .active(true)
                .verified(true)
                .workingHoursWeekday("09:00-18:00")
                .workingHoursWeekend("10:00-16:00")
                .partner(hyper)
                .build();
    }

    @Test
    void profileReturnsPackageCountAndLeavesServiceCountEmpty() {
        ServiceCategory routine = ServiceCategory.builder()
                .id(1L).code("routine").titleJson("{\"az\":\"Dövri\",\"en\":\"Routine care\",\"ru\":\"ТО\"}")
                .active(true).toggleable(false).sortOrder(1).build();
        ServiceCategory repair = ServiceCategory.builder()
                .id(2L).code("repair_inspection").titleJson("{\"en\":\"Inspection & repair\"}")
                .active(true).toggleable(true).sortOrder(2).build();
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(partnerPhotoRepository.existsByPartnerId(1L)).thenReturn(true);
        when(carePackageRepository.countByBranch_IdAndActiveTrue(7L)).thenReturn(1L);
        when(categoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(routine, repair));
        when(branchCategoryRepository.findByBranch_Id(7L)).thenReturn(List.of(
                BranchServiceCategory.builder().category(repair).active(true).build()));

        BookingBranchProfileResponse out = service.profile(7L, 40.380, 49.846, "en");

        assertEquals("Xəqani Express", out.getName());
        assertEquals("Hyper", out.getPartnerName());
        assertTrue(out.getVerified());
        assertEquals("/api/v1/photo/for/partner/get/1", out.getLogoUrl());
        assertEquals("09:00-18:00", out.getWorkingHoursWeekday());
        assertEquals("10:00-16:00", out.getWorkingHoursSaturday());
        assertEquals("10:00-16:00", out.getWorkingHoursSunday());
        assertEquals("10:00-16:00", out.getWorkingHoursWeekend());

        branch.setWorkingHoursSaturday("10:00-14:00");
        branch.setWorkingHoursSunday("12:00-15:00");
        branch.setWorkingHoursWeekend(null);
        BookingBranchProfileResponse split = service.profile(7L, null, null, "en");
        assertEquals("10:00-14:00", split.getWorkingHoursSaturday());
        assertEquals("12:00-15:00", split.getWorkingHoursSunday());
        assertNull(split.getWorkingHoursWeekend());
        assertEquals(2, out.getServices().size());
        BookingBranchProfileServiceView routineView = out.getServices().get(0);
        assertEquals("Routine care", routineView.getName());
        assertEquals(1, routineView.getPackageCount());
        assertNull(routineView.getServiceCount());
        assertNull(routineView.getInfo());
        assertNull(out.getServices().get(1).getPackageCount());
        assertNull(out.getServices().get(1).getServiceCount());
        assertEquals("Find out the problem at the service", out.getServices().get(1).getInfo());
        assertEquals("Inspection & repair", out.getServices().get(1).getName());
        assertEquals("Problemi servisdə öyrən", service.profile(7L, null, null, "az").getServices().get(1).getInfo());
        assertEquals("Узнайте проблему в сервисе", service.profile(7L, null, null, "ru").getServices().get(1).getInfo());
        assertEquals("Problemi servisdə öyrən", service.profile(7L, null, null, null).getServices().get(1).getInfo());
        assertTrue(out.getProducts().isEmpty());
        assertEquals(0.1, out.getDistanceKm(), 0.05);
    }

    @Test
    void hiddenCategoryIsOmitted() {
        ServiceCategory repair = ServiceCategory.builder()
                .id(2L).code("repair_inspection").titleJson("{\"az\":\"Təmir\"}")
                .active(true).toggleable(true).build();
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(partnerPhotoRepository.existsByPartnerId(1L)).thenReturn(false);
        when(carePackageRepository.countByBranch_IdAndActiveTrue(7L)).thenReturn(0L);
        when(categoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(repair));
        when(branchCategoryRepository.findByBranch_Id(7L)).thenReturn(List.of());

        BookingBranchProfileResponse out = service.profile(7L, null, null, "az");

        assertTrue(out.getServices().isEmpty());
        assertNull(out.getLogoUrl());
        assertNull(out.getDistanceKm());
    }

    @Test
    void inactiveBranchIsNotFound() {
        branch.setActive(false);
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        assertThrows(ResourceNotFoundException.class, () -> service.profile(7L, null, null, "az"));
    }

    @Test
    void openUsesTheHoursOfThatDayInBaku() {
        OffsetDateTime morning = OffsetDateTime.parse("2026-09-30T05:30:00Z");
        OffsetDateTime night = OffsetDateTime.parse("2026-09-30T16:30:00Z");
        OffsetDateTime saturdayEarly = OffsetDateTime.parse("2026-10-03T05:00:00Z");
        OffsetDateTime saturdayOpen = OffsetDateTime.parse("2026-10-03T06:30:00Z");
        OffsetDateTime sundayNoon = OffsetDateTime.parse("2026-10-04T08:00:00Z");
        assertEquals(Boolean.TRUE, BookingBranchProfileService.openNow("09:00-18:00", "10:00-16:00", "11:00-15:00", morning));
        assertEquals(Boolean.FALSE, BookingBranchProfileService.openNow("09:00-18:00", "10:00-16:00", "11:00-15:00", night));
        assertEquals(Boolean.FALSE, BookingBranchProfileService.openNow("09:00-18:00", "10:00-16:00", "11:00-15:00", saturdayEarly));
        assertEquals(Boolean.TRUE, BookingBranchProfileService.openNow("09:00-18:00", "10:00-16:00", "11:00-15:00", saturdayOpen));
        assertEquals(Boolean.TRUE, BookingBranchProfileService.openNow("09:00-18:00", "10:00-16:00", "11:00-15:00", sundayNoon));
        assertEquals(Boolean.FALSE, BookingBranchProfileService.openNow("09:00-18:00", "10:00-16:00", null, sundayNoon));
        assertEquals(Boolean.FALSE, BookingBranchProfileService.openNow(null, null, null, morning));
        assertNull(BookingBranchProfileService.openNow("bad", "10:00-16:00", "11:00-15:00", morning));
    }
}
