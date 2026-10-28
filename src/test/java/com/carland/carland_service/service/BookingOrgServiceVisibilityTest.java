package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingStaffOrgResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.entity.BranchCompletion;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOrgServiceVisibilityTest {

    @Mock PartnerRepository partnerRepository;
    @Mock BranchRepository branchRepository;
    @Mock BookingStaffRepository staffRepository;
    @Mock AuthStaffFeign authStaffFeign;
    @Mock BookingStaffAuditService staffAuditService;
    @Mock MailService mailService;

    @InjectMocks BookingOrgService service;

    @Test
    void invitedJwtBlocksUntilPasswordChange() {
        assertThrows(ForbiddenException.class, () -> service.visiblePartner(9L, true));
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

        BookingStaffOrgResponse out = service.visiblePartner(9L, false);
        assertEquals(2, out.getPartner().getBranches().size());
        assertEquals("A", out.getPartner().getBranches().get(0).getName());
        assertEquals("B", out.getPartner().getBranches().get(1).getName());
        assertEquals(BranchCompletion.WEBHOOK, out.getPartner().getBranches().get(0).getCompletionMode());
        assertEquals(BranchCompletion.WEBHOOK, out.getPartner().getBranches().get(1).getCompletionMode());
        assertEquals(0L, out.getPartner().getRatingCount());
        assertNull(out.getPartner().getRating());
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

        BookingStaffOrgResponse out = service.visiblePartner(8L, false);
        assertEquals(1, out.getPartner().getBranches().size());
        assertEquals("Mine", out.getPartner().getBranches().get(0).getName());
        assertEquals(10L, out.getPartner().getBranches().get(0).getId());
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
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.visiblePartner(9L, false));
        assertEquals("Əməkdaşlıq dayandırılıb", ex.getMessage());
    }

    @Test
    void partnerReopenDoesNotCallStaffHireBack() {
        Partner partner = Partner.builder().id(1L).active(false).source("hyper").build();
        Branch branch = Branch.builder().id(10L).active(false).partner(partner).build();
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
        when(branchRepository.findByPartnerIdOrderByIdAsc(1L)).thenReturn(List.of(branch));

        service.setPartnerActive(1L, true);

        assertEquals(true, partner.getActive());
        assertEquals(true, branch.getActive());
        verify(authStaffFeign, never()).setManual(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginGateKeepsHandClosedStaffOut() {
        Partner partner = Partner.builder().id(1L).active(true).source("hyper").build();
        BookingStaff fired = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .manuallyDisabled(true)
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
        when(staffRepository.findByUserId(9L)).thenReturn(List.of(fired));
        assertEquals("SUSPENDED", service.loginGate(9L).getResult());
    }

    @Test
    void loginGateSuspendsBranchStaffWhenBranchClosed() {
        Partner partner = Partner.builder().id(1L).active(true).source("hyper").build();
        Branch branch = Branch.builder().id(4L).active(false).partner(partner).build();
        BookingStaff row = BookingStaff.builder()
                .userId(8L)
                .partner(partner)
                .branch(branch)
                .status(BookingStaffStatus.ACTIVE.name())
                .role(BookingStaffRole.BRANCH_ADMIN.name())
                .build();
        when(staffRepository.findByUserId(8L)).thenReturn(List.of(row));
        assertEquals("SUSPENDED", service.loginGate(8L).getResult());
    }

    @Test
    void loginGateAllowsInvitedStaffWhenPlaceIsOpen() {
        Partner partner = Partner.builder().id(1L).active(true).source("hyper").build();
        BookingStaff row = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .status(BookingStaffStatus.INVITED.name())
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .build();
        when(staffRepository.findByUserId(9L)).thenReturn(List.of(row));
        assertEquals("ALLOWED", service.loginGate(9L).getResult());
    }

    @Test
    void newBranchOpensOnDetailedCompletion() {
        Partner partner = Partner.builder().id(1L).name("Castrol").build();
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
        when(branchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Branch saved = service.addBranch(1L, "Baku", null, null, null, true,
                null, null, null, null, false, null);

        assertEquals(BranchCompletion.DETAILED, saved.getCompletionMode());
    }

    @Test
    void completionSwitchWritesTheChosenMode() {
        Partner partner = Partner.builder().id(1L).build();
        Branch branch = Branch.builder().id(7L).partner(partner).name("Babek").build();
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(BranchCompletion.DETAILED, service.setBranchCompletion(1L, 7L, true).getCompletionMode());
        assertEquals(BranchCompletion.WEBHOOK, service.setBranchCompletion(1L, 7L, false).getCompletionMode());
    }

    @Test
    void updateBranchWritesNameAddressPhoneAndPlace() {
        Partner partner = Partner.builder().id(1L).name("HS").build();
        Branch branch = Branch.builder().id(10L).name("Old").partner(partner).verified(false).build();
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Branch saved = service.updateBranchHours(1L, 10L, true,
                "09:00-18:00", "10:00-16:00", null,
                "  Yeni  ", "  Nizami  ", " +994701112233 ",
                40.37, 49.83);

        assertEquals("Yeni", saved.getName());
        assertEquals("Nizami", saved.getAddress());
        assertEquals("+994701112233", saved.getContactPhone());
        assertEquals(40.37, saved.getLat());
        assertEquals(49.83, saved.getLng());
        assertTrue(saved.getVerified());
        assertEquals("09:00-18:00", saved.getWorkingHoursWeekday());
    }

    @Test
    void updateBranchRejectsBlankName() {
        Partner partner = Partner.builder().id(1L).build();
        Branch branch = Branch.builder().id(10L).name("Old").partner(partner).build();
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));

        assertThrows(MissingFieldException.class, () -> service.updateBranchHours(
                1L, 10L, false, null, null, null, "  ", null, null, null, null));
        verify(branchRepository, never()).save(any());
    }

    @Test
    void completionSwitchRejectsAnotherPartnersBranch() {
        Partner owner = Partner.builder().id(2L).build();
        Branch branch = Branch.builder().id(7L).partner(owner).name("Babek").build();
        when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));

        assertThrows(ResourceNotFoundException.class, () -> service.setBranchCompletion(1L, 7L, true));
        verify(branchRepository, never()).save(any());
    }
}
