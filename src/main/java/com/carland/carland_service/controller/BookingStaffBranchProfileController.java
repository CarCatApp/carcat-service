package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffBranchProfileView;
import com.carland.carland_service.dto.request.StaffBranchProfileSaveRequest;
import com.carland.carland_service.dto.request.StaffBrandModelSaveRequest;
import com.carland.carland_service.dto.request.StaffBranchWorkingHoursRequest;
import com.carland.carland_service.dto.request.StaffNameSaveRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.BranchProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli şube profili. Marka satırları ortaq başlığın altındadır.
 * en: Partner-panel branch profile. Brand rows sit under a shared heading.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffBranchProfileController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final BranchProfileService branchProfileService;

    @GetMapping("/api/v1/booking/staff/branch/profile")
    public StaffBranchProfileView get(
            HttpServletRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.get(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                acceptLanguage);
    }

    @PatchMapping("/api/v1/booking/staff/branch/profile")
    public StaffBranchProfileView updateProfile(
            HttpServletRequest request,
            @RequestBody StaffBranchProfileSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.updateProfile(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }

    @PatchMapping("/api/v1/booking/staff/branch/working-hours")
    public StaffBranchProfileView updateWorkingHours(
            HttpServletRequest request,
            @RequestBody StaffBranchWorkingHoursRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.updateWorkingHours(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }

    @PatchMapping("/api/v1/booking/staff/branch/profile/email")
    public StaffBranchProfileView updateContactEmail(
            HttpServletRequest request,
            @RequestBody StaffBranchProfileSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.updateContactEmail(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }

    @PatchMapping("/api/v1/booking/staff/me")
    public StaffBranchProfileView updateStaffName(
            HttpServletRequest request,
            @RequestBody StaffNameSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.updateStaffName(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }

    @PostMapping("/api/v1/booking/staff/branch/brand-model-services/{serviceId}/models")
    public StaffBranchProfileView addBrandModel(
            HttpServletRequest request,
            @PathVariable Long serviceId,
            @RequestBody StaffBrandModelSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.addBrandModel(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                serviceId,
                body,
                acceptLanguage);
    }

    @PatchMapping("/api/v1/booking/staff/branch/brand-model-services/{serviceId}/models/{modelId}")
    public StaffBranchProfileView updateBrandModel(
            HttpServletRequest request,
            @PathVariable Long serviceId,
            @PathVariable Long modelId,
            @RequestBody StaffBrandModelSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.updateBrandModel(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                serviceId,
                modelId,
                body,
                acceptLanguage);
    }

    @DeleteMapping("/api/v1/booking/staff/branch/brand-model-services/{serviceId}/models/{modelId}")
    public StaffBranchProfileView deleteBrandModel(
            HttpServletRequest request,
            @PathVariable Long serviceId,
            @PathVariable Long modelId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchProfileService.deleteBrandModel(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                serviceId,
                modelId,
                acceptLanguage);
    }
}
