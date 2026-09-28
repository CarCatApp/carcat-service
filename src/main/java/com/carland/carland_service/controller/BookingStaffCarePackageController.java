package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffCarePackageListResponse;
import com.carland.carland_service.dto.booking.StaffCarePackageView;
import com.carland.carland_service.dto.request.StaffCarePackageActiveRequest;
import com.carland.carland_service.dto.request.StaffCarePackageSaveRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.BranchCarePackageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli şube paketleri. Auth BookingStaffApiController ile aynı.
 * en: Partner-panel branch packages. Auth matches BookingStaffApiController.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffCarePackageController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final BranchCarePackageService branchCarePackageService;

    /**
     * tr: Bu şubenin paketleri.
     * en: Packages for this branch.
     */
    @GetMapping("/api/v1/booking/staff/catalog/care-packages")
    public StaffCarePackageListResponse list(
            HttpServletRequest request,
            @RequestParam(required = false) Long branchId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchCarePackageService.list(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                branchId,
                acceptLanguage);
    }

    /**
     * tr: Seçilen xidmətlerle paket oluşturur.
     * en: Creates a package from the selected services.
     */
    @PostMapping("/api/v1/booking/staff/catalog/care-packages")
    public StaffCarePackageView create(
            HttpServletRequest request,
            @RequestBody StaffCarePackageSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchCarePackageService.create(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }

    /**
     * tr: Ad, fiyat ve açık xidmətləri günceller.
     * en: Updates the name, price, and services switched on.
     */
    @PutMapping("/api/v1/booking/staff/catalog/care-packages/{packageId}")
    public StaffCarePackageView update(
            HttpServletRequest request,
            @PathVariable Long packageId,
            @RequestBody StaffCarePackageSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchCarePackageService.update(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                packageId,
                body,
                acceptLanguage);
    }

    /**
     * tr: Paketi deaktiv və ya aktiv edir.
     * en: Deactivates or activates the package.
     */
    @PatchMapping("/api/v1/booking/staff/catalog/care-packages/{packageId}/active")
    public StaffCarePackageView setActive(
            HttpServletRequest request,
            @PathVariable Long packageId,
            @RequestBody StaffCarePackageActiveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchCarePackageService.setActive(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                packageId,
                body == null ? null : body.getActive(),
                acceptLanguage);
    }
}
