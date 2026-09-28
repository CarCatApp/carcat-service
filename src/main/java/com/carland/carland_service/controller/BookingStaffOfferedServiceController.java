package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffOfferedServiceListResponse;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.OfferedCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli dövri qulluq xidmət listesi. Auth BookingStaffApiController ile aynı.
 * en: Partner-panel routine-care service list. Auth matches BookingStaffApiController.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffOfferedServiceController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final OfferedCatalogService offeredCatalogService;

    /**
     * tr: Aktif gruplar ve altındaki xidmətlər.
     * en: Active groups and the services under them.
     */
    @GetMapping("/api/v1/booking/staff/catalog/offered-services")
    public StaffOfferedServiceListResponse list(
            HttpServletRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return offeredCatalogService.listForStaff(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                acceptLanguage);
    }
}
