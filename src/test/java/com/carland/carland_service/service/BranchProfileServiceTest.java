package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBranchProfileView;
import com.carland.carland_service.dto.request.StaffBrandModelSaveRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchGoodRepository;
import com.carland.carland_service.repository.BranchPhotoRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.repository.StaffPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchProfileServiceTest {

    @Mock BookingStaffAccess bookingStaffAccess;
    @Mock BookingStaffRepository bookingStaffRepository;
    @Mock BranchRepository branchRepository;
    @Mock BranchGoodRepository branchGoodRepository;
    @Mock BrandModelServiceRepository brandModelServiceRepository;
    @Mock BrandModelRepository brandModelRepository;
    @Mock BranchPhotoRepository branchPhotoRepository;
    @Mock StaffPhotoRepository staffPhotoRepository;
    @Mock RedisCacheService redisCacheService;

    BranchProfileService profile;
    StaffMediaService media;
    BookingStaff partnerAdmin;
    BookingStaff branchAdmin;
    Branch branch;
    BrandModelService oilHeading;
    BrandModelService filterHeading;

    @BeforeEach
    void setUp() {
        profile = new BranchProfileService(
                bookingStaffAccess,
                bookingStaffRepository,
                branchRepository,
                branchGoodRepository,
                brandModelServiceRepository,
                brandModelRepository,
                branchPhotoRepository,
                staffPhotoRepository);
        media = new StaffMediaService(
                bookingStaffAccess,
                branchRepository,
                branchPhotoRepository,
                staffPhotoRepository,
                redisCacheService);
        Partner partner = Partner.builder().id(3L).build();
        branch = Branch.builder().id(12L).partner(partner).name("Hyper").build();
        partnerAdmin = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .name("Nemat")
                .surname("Mirzayev")
                .build();
        branchAdmin = BookingStaff.builder()
                .userId(8L)
                .partner(partner)
                .branch(branch)
                .role(BookingStaffRole.BRANCH_ADMIN.name())
                .name("Nemat")
                .surname("Mirzayev")
                .build();
        oilHeading = BrandModelService.builder().id(1L).branch(branch).title("Yağ").oil(true).sortOrder(0).build();
        filterHeading = BrandModelService.builder().id(2L).branch(branch).title("Filter").oil(false).sortOrder(1).build();
    }

    @Test
    void partnerAdminCannotUploadBranchPhoto() {
        when(bookingStaffAccess.requireStaff(9L, false, "az")).thenReturn(partnerAdmin);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});

        assertThrows(ForbiddenException.class,
                () -> media.uploadBranchPhoto(9L, false, file, "az"));
        verify(branchPhotoRepository, never()).save(any());
    }

    @Test
    void nonOilModelKeepsSeriesAndViscosityNull() {
        when(bookingStaffAccess.requireStaff(8L, false, "az")).thenReturn(branchAdmin);
        when(brandModelServiceRepository.findById(2L)).thenReturn(Optional.of(filterHeading));
        when(branchGoodRepository.findByBranch_IdOrderBySortOrderAscIdAsc(12L)).thenReturn(List.of());
        when(brandModelServiceRepository.findByBranch_IdOrderBySortOrderAscIdAsc(12L)).thenReturn(List.of());
        when(branchPhotoRepository.existsByBranchId(12L)).thenReturn(false);
        when(staffPhotoRepository.existsByUserId(8L)).thenReturn(false);

        profile.addBrandModel(8L, false, 2L, StaffBrandModelSaveRequest.builder()
                .name("Mann")
                .series("should drop")
                .viscosity("5W-30")
                .unit("eded")
                .build(), "az");

        ArgumentCaptor<BrandModel> captor = ArgumentCaptor.forClass(BrandModel.class);
        verify(brandModelRepository).save(captor.capture());
        assertEquals("Mann", captor.getValue().getName());
        assertEquals("eded", captor.getValue().getUnit());
        assertNull(captor.getValue().getSeries());
        assertNull(captor.getValue().getViscosity());
    }

    @Test
    void oilModelForcesLiterAndKeepsViscosity() {
        when(bookingStaffAccess.requireStaff(8L, false, "az")).thenReturn(branchAdmin);
        when(brandModelServiceRepository.findById(1L)).thenReturn(Optional.of(oilHeading));
        when(branchGoodRepository.findByBranch_IdOrderBySortOrderAscIdAsc(12L)).thenReturn(List.of());
        when(brandModelServiceRepository.findByBranch_IdOrderBySortOrderAscIdAsc(12L)).thenReturn(List.of());
        when(branchPhotoRepository.existsByBranchId(12L)).thenReturn(false);
        when(staffPhotoRepository.existsByUserId(8L)).thenReturn(false);

        StaffBranchProfileView view = profile.addBrandModel(8L, false, 1L, StaffBrandModelSaveRequest.builder()
                .name("Liqui Moly")
                .series("Top Tec")
                .viscosity("5W-30")
                .unit("kq")
                .build(), "az");

        ArgumentCaptor<BrandModel> captor = ArgumentCaptor.forClass(BrandModel.class);
        verify(brandModelRepository).save(captor.capture());
        assertEquals("kq", captor.getValue().getUnit());
        assertEquals("Top Tec", captor.getValue().getSeries());
        assertEquals("5W-30", captor.getValue().getViscosity());
        assertEquals(Boolean.TRUE, view.getCanUploadBranchPhoto());
    }
}
