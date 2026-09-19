package com.carland.carland_service.service;

import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.BookingStaffAudit;
import com.carland.carland_service.repository.BookingStaffAuditRepository;
import com.carland.carland_service.repository.BookingStaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingStaffAuditServiceTest {

    @Mock BookingStaffAuditRepository auditRepository;
    @Mock BookingStaffRepository staffRepository;
    @InjectMocks BookingStaffAuditService service;

    @Test
    void createDoesNotStorePassword() {
        when(auditRepository.save(any(BookingStaffAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        BookingStaffAudit row = service.recordCreate("+994500000000", 9L, 1L, 10L, "+994701111111", "BRANCH_ADMIN");
        assertEquals(BookingStaffAuditService.CREATE, row.getAction());
        assertEquals(9L, row.getUserId());
        assertTrue(row.getDetail() == null || !row.getDetail().toLowerCase().contains("password"));
        ArgumentCaptor<BookingStaffAudit> cap = ArgumentCaptor.forClass(BookingStaffAudit.class);
        verify(auditRepository).save(cap.capture());
        assertEquals("staff created", cap.getValue().getDetail());
    }

    @Test
    void loginResolvesPartnerFromMembership() {
        Partner partner = Partner.builder().id(4L).name("HS").active(true).source("hyper").build();
        BookingStaff staff = BookingStaff.builder()
                .userId(8L)
                .partner(partner)
                .role("PARTNER_ADMIN")
                .phoneNumber("+994702222222")
                .build();
        when(staffRepository.findByUserId(8L)).thenReturn(List.of(staff));
        when(auditRepository.save(any(BookingStaffAudit.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingStaffAudit row = service.record(com.carland.carland_service.dto.booking.StaffAuditRequest.builder()
                .action(BookingStaffAuditService.LOGIN)
                .actor("+994702222222")
                .userId(8L)
                .success(true)
                .detail("OK")
                .build());
        assertEquals(4L, row.getPartnerId());
        assertEquals("PARTNER_ADMIN", row.getRole());
        assertEquals(Boolean.TRUE, row.getSuccess());
    }

    @Test
    void roleChangeWritesFromToWithoutSecret() {
        when(auditRepository.save(any(BookingStaffAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        BookingStaffAudit row = service.recordRoleChange(
                "+994500000000", 9L, 1L, null, "+994701111111", "BRANCH_ADMIN", "PARTNER_ADMIN");
        assertEquals(BookingStaffAuditService.ROLE_CHANGE, row.getAction());
        assertEquals("BRANCH_ADMIN -> PARTNER_ADMIN", row.getDetail());
        assertEquals("PARTNER_ADMIN", row.getRole());
    }
}
