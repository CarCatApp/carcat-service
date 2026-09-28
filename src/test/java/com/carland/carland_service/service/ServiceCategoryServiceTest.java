package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffCatalogCategoryListResponse;
import com.carland.carland_service.dto.booking.StaffCatalogCategoryView;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.ServiceCategory;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceCategoryRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.ServiceCategoryPhotoRepository;
import com.carland.carland_service.repository.ServiceCategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCategoryServiceTest {

    @Mock ServiceCategoryRepository categoryRepository;
    @Mock BranchServiceCategoryRepository branchCategoryRepository;
    @Mock ServiceCategoryPhotoRepository photoRepository;
    @Mock BranchServiceRepository branchServiceRepository;
    @Mock BranchRepository branchRepository;
    @Mock BookingStaffAccess bookingStaffAccess;

    ServiceCategoryService service;
    BookingStaff staff;
    Branch branch;
    ServiceCategory routine;
    ServiceCategory repair;

    @BeforeEach
    void setUp() {
        service = new ServiceCategoryService(
                categoryRepository,
                branchCategoryRepository,
                photoRepository,
                branchServiceRepository,
                branchRepository,
                bookingStaffAccess,
                new ServiceCategoryJson(new ObjectMapper()));
        Partner partner = Partner.builder().id(3L).build();
        branch = Branch.builder().id(12L).partner(partner).build();
        staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .build();
        routine = ServiceCategory.builder()
                .id(1L)
                .code("routine")
                .titleJson("{\"az\":\"Dövri Qulluq\",\"en\":\"Routine Care\",\"ru\":\"Плановое обслуживание\"}")
                .descriptionJson("{\"az\":\"Paketlər\",\"en\":\"Packages\",\"ru\":\"Пакеты\"}")
                .sortOrder(1)
                .openable(true)
                .toggleable(false)
                .active(true)
                .build();
        repair = ServiceCategory.builder()
                .id(2L)
                .code("repair_inspection")
                .titleJson("{\"az\":\"Təmir\",\"en\":\"Repair\",\"ru\":\"Ремонт\"}")
                .sortOrder(2)
                .openable(false)
                .toggleable(true)
                .active(true)
                .directionKeys("dir:repair,dir:inspection")
                .build();
    }

    @Test
    void listMarksUntoggleableAlwaysOnAndMissingBranchRowOff() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(branchRepository.findByPartnerOrderByIdAsc(staff.getPartner())).thenReturn(List.of(branch));
        when(branchCategoryRepository.findByBranch_Id(12L)).thenReturn(List.of());
        when(categoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(routine, repair));
        when(photoRepository.existsByCategoryId(1L)).thenReturn(true);
        when(photoRepository.existsByCategoryId(2L)).thenReturn(false);

        StaffCatalogCategoryListResponse response = service.list(9L, false, null, "az");

        assertEquals(12L, response.getBranchId());
        assertEquals(2, response.getItems().size());
        StaffCatalogCategoryView first = response.getItems().get(0);
        assertEquals("Dövri Qulluq", first.getTitle().get("az"));
        assertTrue(first.getActive());
        assertFalse(first.getToggleable());
        assertEquals("/api/v1/photo/for/service-category/get?categoryId=1", first.getIconUrl());
        StaffCatalogCategoryView second = response.getItems().get(1);
        assertFalse(second.getActive());
        assertNull(second.getIconUrl());
    }

    @Test
    void toggleRejectsCategoryThatCannotBeTurnedOff() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 12L, "az")).thenReturn(branch);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(routine));

        assertThrows(ConflictException.class, () -> service.toggle(9L, false, 1L, 12L, true, "az"));
    }

    @Test
    void toggleTurnsOnRepairAndMatchingDirections() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(staff);
        when(bookingStaffAccess.requireWritableBranch(staff, 12L, "az")).thenReturn(branch);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(repair));
        when(branchCategoryRepository.findByBranch_IdAndCategory_Id(12L, 2L)).thenReturn(Optional.empty());
        when(branchServiceRepository.findByBranch_IdAndServiceKey(12L, "dir:repair")).thenReturn(Optional.empty());
        when(branchServiceRepository.findByBranch_IdAndServiceKey(12L, "dir:inspection")).thenReturn(Optional.empty());
        when(photoRepository.existsByCategoryId(2L)).thenReturn(false);

        StaffCatalogCategoryView view = service.toggle(9L, false, 2L, 12L, true, "az");

        assertTrue(view.getActive());
        ArgumentCaptor<BranchService> saved = ArgumentCaptor.forClass(BranchService.class);
        verify(branchServiceRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(List.of("dir:repair", "dir:inspection"),
                saved.getAllValues().stream().map(BranchService::getServiceKey).toList());
        assertTrue(saved.getAllValues().stream().allMatch(row -> Boolean.TRUE.equals(row.getActive())));
        assertTrue(saved.getAllValues().stream().allMatch(row -> "DIRECTION".equals(row.getKind())));
        verify(branchCategoryRepository).save(any());
    }
}
