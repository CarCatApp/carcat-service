package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingInboxResponse;
import com.carland.carland_service.dto.booking.BookingStaffOrgResponse;
import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.StaffAuditRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.security.InternalTokenValidator;
import com.carland.carland_service.security.WebhookAuthFailure;
import com.carland.carland_service.security.WebhookAuthValidationResult;
import com.carland.carland_service.service.BookingOrgService;
import com.carland.carland_service.service.BookingStaffAuditService;
import com.carland.carland_service.service.StaffBookingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookingStaffApiController {

    private final BookingOrgService bookingOrgService;
    private final BookingStaffAuditService staffAuditService;
    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final InternalTokenValidator internalTokenValidator;
    private final StaffBookingService staffBookingService;

    @GetMapping("/api/v1/booking/staff/branches")
    public BookingStaffOrgResponse myBranches(HttpServletRequest request) {
        Long userId = bookingStaffRequestAuth.userId(request);
        boolean mustChange = bookingStaffRequestAuth.mustChangePassword(request);
        return bookingOrgService.visiblePartner(userId, mustChange);
    }

    @GetMapping("/api/v1/booking/staff/bookings")
    public BookingInboxResponse inbox(
            HttpServletRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return staffBookingService.inbox(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                status, branchId, page, pageSize, timezone, acceptLanguage);
    }

    @PostMapping("/api/v1/booking/staff/bookings/{bookingId}/accept")
    public BookingView accept(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone
    ) {
        return staffBookingService.accept(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, timezone, acceptLanguage);
    }

    @PostMapping("/api/v1/booking/staff/bookings/{bookingId}/reject")
    public BookingView reject(
            HttpServletRequest request,
            @PathVariable Long bookingId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @RequestBody(required = false) BookingRejectRequest body
    ) {
        return staffBookingService.reject(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                bookingId, timezone, acceptLanguage);
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
