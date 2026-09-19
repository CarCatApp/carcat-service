package com.carland.carland_service.service;

import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingStaffAccessTest {

    @Mock BookingStaffRepository staffRepository;
    @Mock BranchRepository branchRepository;

    @InjectMocks BookingStaffAccess access;

    @Test
    void partnerAdminCanWriteAnyPartnerBranch() {
        Partner partner = Partner.builder().id(1L).name("HS").build();
        Branch branch = Branch.builder().id(20L).name("B").partner(partner).build();
        BookingStaff staff = BookingStaff.builder()
                .userId(9L)
                .partner(partner)
                .role(BookingStaffRole.PARTNER_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
        when(branchRepository.findById(20L)).thenReturn(Optional.of(branch));

        assertEquals(branch, access.requireWritableBranch(staff, 20L, "az"));
    }

    @Test
    void branchAdminCannotWriteOtherBranch() {
        Partner partner = Partner.builder().id(1L).name("HS").build();
        Branch mine = Branch.builder().id(10L).name("Mine").partner(partner).build();
        Branch other = Branch.builder().id(11L).name("Other").partner(partner).build();
        BookingStaff staff = BookingStaff.builder()
                .userId(8L)
                .partner(partner)
                .branch(mine)
                .role(BookingStaffRole.BRANCH_ADMIN.name())
                .status(BookingStaffStatus.ACTIVE.name())
                .build();
        when(branchRepository.findById(11L)).thenReturn(Optional.of(other));

        assertThrows(InvalidStatusException.class, () -> access.requireWritableBranch(staff, 11L, "az"));
    }

    @Test
    void isActiveStaffIgnoresInactiveRows() {
        BookingStaff inactive = BookingStaff.builder()
                .userId(7L)
                .status("INACTIVE")
                .build();
        when(staffRepository.findByUserId(7L)).thenReturn(List.of(inactive));
        assertFalse(access.isActiveStaff(7L));
        assertFalse(access.isActiveStaff(null));
    }
}
