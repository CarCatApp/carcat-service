package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBranchView;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.feign.AuthStaffFeign;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOrgServiceVisibilityTest {

    @Mock PartnerRepository partnerRepository;
    @Mock BranchRepository branchRepository;
    @Mock BookingStaffRepository staffRepository;
    @Mock AuthStaffFeign authStaffFeign;
    @Mock BookingStaffAuditService staffAuditService;

    @InjectMocks BookingOrgService service;

    @Test
    void invitedJwtBlocksUntilPasswordChange() {
        assertThrows(ForbiddenException.class, () -> service.visibleBranches(9L, true));
    }

    @Test
    void partnerAdminSeesAllBranches() {
        Partner partner = Partner.builder().id(1L).name("HS").active(true).source("hyper").build();
        Branch a = Branch.builder().id(10L).name("A").partner(partner).active(true).build();
        Branch b = Branch.builder().id(11L).name("B").partner(partner).active(false).build();
        BookingStaff hq = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .phoneNumber("+994701111111")
                .build();
        when(staffRepository.findByUserId(9L)).thenReturn(List.of(hq));
        when(branchRepository.findByPartnerIdOrderByIdAsc(1L)).thenReturn(List.of(a, b));

        List<BookingBranchView> views = service.visibleBranches(9L, false);
        assertEquals(2, views.size());
        assertEquals("A", views.get(0).getName());
        assertEquals("B", views.get(1).getName());
    }

    @Test
    void branchAdminSeesOnlyOwnBranch() {
        Partner partner = Partner.builder().id(1L).name("HS").active(true).source("hyper").build();
        Branch mine = Branch.builder().id(10L).name("Mine").partner(partner).active(true).build();
        BookingStaff row = BookingStaff.builder()
                .userId(8L)
                .partner(partner)
                .branch(mine)
                .role(BookingStaffRole.BRANCH_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .phoneNumber("+994702222222")
                .build();
        when(staffRepository.findByUserId(8L)).thenReturn(List.of(row));

        List<BookingBranchView> views = service.visibleBranches(8L, false);
        assertEquals(1, views.size());
        assertEquals("Mine", views.get(0).getName());
    }

    @Test
    void inactivePartnerHidesBranches() {
        Partner partner = Partner.builder().id(1L).name("HS").active(false).source("hyper").build();
        BookingStaff hq = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .phoneNumber("+994701111111")
                .build();
        when(staffRepository.findByUserId(9L)).thenReturn(List.of(hq));
        assertTrue(service.visibleBranches(9L, false).isEmpty());
    }
}
