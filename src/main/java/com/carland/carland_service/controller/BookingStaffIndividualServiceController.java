package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffIndividualServiceListResponse;
import com.carland.carland_service.dto.booking.StaffIndividualServiceView;
import com.carland.carland_service.dto.request.StaffIndividualServiceActiveRequest;
import com.carland.carland_service.dto.request.StaffIndividualServicePricesRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.IndividualCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli fərdi xidmət fiyatları. Auth paket uçlarıyla aynı.
 * en: Partner-panel individual-service prices. Auth matches the package endpoints.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffIndividualServiceController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final IndividualCatalogService individualCatalogService;

    /**
     * tr: Katalog ve bu şubenin fiyat/durumu. Kapalı satırlar da gelir.
     * en: Catalog plus this branch's prices and status. Turned-off rows are included.
     */
    @GetMapping("/api/v1/booking/staff/catalog/individual-services")
    public StaffIndividualServiceListResponse list(
            HttpServletRequest request,
            @RequestParam(required = false) Long branchId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return individualCatalogService.listForStaff(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                branchId,
                acceptLanguage);
    }

    /**
     * tr: Şube xidmətini açar veya kapatır. Fiyatlar kalır.
     * en: Turns the branch service on or off. Prices stay.
     */
    @PatchMapping("/api/v1/booking/staff/catalog/individual-services/{serviceId}/active")
    public StaffIndividualServiceView setActive(
            HttpServletRequest request,
            @PathVariable Long serviceId,
            @RequestBody StaffIndividualServiceActiveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return individualCatalogService.setActive(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                serviceId,
                body == null ? null : body.getActive(),
                acceptLanguage);
    }

    /**
     * tr: Üç fiyatı kaydeder. Durumu değiştirmez.
     * en: Saves the three prices. Status is not changed.
     */
    @PutMapping("/api/v1/booking/staff/catalog/individual-services/{serviceId}/prices")
    public StaffIndividualServiceView updatePrices(
            HttpServletRequest request,
            @PathVariable Long serviceId,
            @RequestBody StaffIndividualServicePricesRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return individualCatalogService.updatePrices(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                serviceId,
                body,
                acceptLanguage);
    }
}
