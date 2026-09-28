package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffCatalogCategoryListResponse;
import com.carland.carland_service.dto.booking.StaffCatalogCategoryToggleRequest;
import com.carland.carland_service.dto.booking.StaffCatalogCategoryView;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.ServiceCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli hizmet kategorileri. Auth BookingStaffApiController ile aynı.
 * en: Partner-panel service categories. Auth matches BookingStaffApiController.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffCatalogController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final ServiceCategoryService serviceCategoryService;

    /**
     * tr: Şubenin göreceği aktif kategoriler. branchId opsiyonel.
     * en: Active categories for the resolved branch. branchId is optional.
     */
    @GetMapping("/api/v1/booking/staff/catalog/categories")
    public StaffCatalogCategoryListResponse list(
            HttpServletRequest request,
            @RequestParam(required = false) Long branchId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return serviceCategoryService.list(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                branchId,
                acceptLanguage);
    }

    /**
     * tr: Kategoriyi şube için açar veya kapatır. Toggleable değilse 409.
     * en: Turns a category on or off for the branch. 409 when the category is not toggleable.
     */
    @PatchMapping("/api/v1/booking/staff/catalog/categories/{categoryId}")
    public StaffCatalogCategoryView toggle(
            HttpServletRequest request,
            @PathVariable Long categoryId,
            @RequestBody StaffCatalogCategoryToggleRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        Long branchId = body == null ? null : body.getBranchId();
        Boolean active = body == null ? null : body.getActive();
        return serviceCategoryService.toggle(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                categoryId,
                branchId,
                active,
                acceptLanguage);
    }
}
