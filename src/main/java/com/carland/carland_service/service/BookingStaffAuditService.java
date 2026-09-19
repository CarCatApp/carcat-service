package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffAuditRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.BookingStaffAudit;
import com.carland.carland_service.repository.BookingStaffAuditRepository;
import com.carland.carland_service.repository.BookingStaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingStaffAuditService {

    public static final String CREATE = "CREATE";
    public static final String LOGIN = "LOGIN";
    public static final String ROLE_CHANGE = "ROLE_CHANGE";

    private final BookingStaffAuditRepository auditRepository;
    private final BookingStaffRepository staffRepository;

    @Transactional
    public BookingStaffAudit record(StaffAuditRequest request) {
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            return null;
        }
        String action = request.getAction().trim().toUpperCase();
        Long partnerId = request.getPartnerId();
        Long branchId = request.getBranchId();
        String role = request.getRole();
        String phone = request.getPhoneNumber();
        if (request.getUserId() != null && (partnerId == null || role == null)) {
            List<BookingStaff> rows = staffRepository.findByUserId(request.getUserId());
            if (!rows.isEmpty()) {
                BookingStaff row = rows.get(0);
                if (partnerId == null && row.getPartner() != null) {
                    partnerId = row.getPartner().getId();
                }
                if (branchId == null && row.getBranch() != null) {
                    branchId = row.getBranch().getId();
                }
                if (role == null) {
                    role = row.getRole();
                }
                if (phone == null) {
                    phone = row.getPhoneNumber();
                }
            }
        }
        BookingStaffAudit saved = auditRepository.save(BookingStaffAudit.builder()
                .createdAt(LocalDateTime.now())
                .actor(blankToUnknown(request.getActor()))
                .action(action)
                .userId(request.getUserId())
                .partnerId(partnerId)
                .branchId(branchId)
                .phoneNumber(phone)
                .role(role)
                .detail(sanitize(request.getDetail()))
                .success(request.getSuccess())
                .build());
        log.info("BOOKING_STAFF_AUDIT action={} userId={} partnerId={} actor={}",
                action, saved.getUserId(), saved.getPartnerId(), saved.getActor());
        return saved;
    }

    public BookingStaffAudit recordCreate(String actor, Long userId, Long partnerId, Long branchId,
                                          String phone, String role) {
        return record(StaffAuditRequest.builder()
                .action(CREATE)
                .actor(actor)
                .userId(userId)
                .partnerId(partnerId)
                .branchId(branchId)
                .phoneNumber(phone)
                .role(role)
                .detail("staff created")
                .success(true)
                .build());
    }

    public BookingStaffAudit recordRoleChange(String actor, Long userId, Long partnerId, Long branchId,
                                              String phone, String fromRole, String toRole) {
        return record(StaffAuditRequest.builder()
                .action(ROLE_CHANGE)
                .actor(actor)
                .userId(userId)
                .partnerId(partnerId)
                .branchId(branchId)
                .phoneNumber(phone)
                .role(toRole)
                .detail((fromRole == null ? "?" : fromRole) + " -> " + (toRole == null ? "?" : toRole))
                .success(true)
                .build());
    }

    @Transactional(readOnly = true)
    public List<BookingStaffAudit> listForPartner(Long partnerId) {
        return auditRepository.findTop50ByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    private static String blankToUnknown(String actor) {
        if (actor == null || actor.isBlank()) {
            return "unknown";
        }
        return actor.trim();
    }

    private static String sanitize(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        String trimmed = detail.trim();
        if (trimmed.length() > 256) {
            return trimmed.substring(0, 256);
        }
        return trimmed;
    }
}
