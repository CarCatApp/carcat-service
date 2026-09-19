package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingBranchView;
import com.carland.carland_service.dto.booking.StaffAuditRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.security.InternalTokenValidator;
import com.carland.carland_service.security.WebhookAuthFailure;
import com.carland.carland_service.security.WebhookAuthValidationResult;
import com.carland.carland_service.service.BookingOrgService;
import com.carland.carland_service.service.BookingStaffAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookingStaffApiController {

    private final BookingOrgService bookingOrgService;
    private final BookingStaffAuditService staffAuditService;
    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final InternalTokenValidator internalTokenValidator;

    @GetMapping("/api/v1/booking/staff/branches")
    public List<BookingBranchView> myBranches(HttpServletRequest request) {
        Long userId = bookingStaffRequestAuth.userId(request);
        boolean mustChange = bookingStaffRequestAuth.mustChangePassword(request);
        return bookingOrgService.visibleBranches(userId, mustChange);
    }

    @PostMapping("/api/v1/internal/booking/staff/activate")
    public ResponseEntity<?> activate(@RequestParam Long userId, HttpServletRequest request) {
        WebhookAuthValidationResult result = internalTokenValidator.validate(request);
        if (!result.isValid()) {
            HttpStatus status = result.getFailure() == WebhookAuthFailure.INTERNAL_TOKEN_NOT_CONFIGURED
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(Map.of("error", result.getFailure().name()));
        }
        bookingOrgService.activateStaff(userId);
        return ResponseEntity.ok(Map.of("status", "ACTIVE"));
    }

    @PostMapping("/api/v1/internal/booking/staff/audit")
    public ResponseEntity<?> audit(@RequestBody StaffAuditRequest body, HttpServletRequest request) {
        WebhookAuthValidationResult result = internalTokenValidator.validate(request);
        if (!result.isValid()) {
            HttpStatus status = result.getFailure() == WebhookAuthFailure.INTERNAL_TOKEN_NOT_CONFIGURED
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(Map.of("error", result.getFailure().name()));
        }
        staffAuditService.record(body);
        return ResponseEntity.ok(Map.of("status", "RECORDED"));
    }
}
