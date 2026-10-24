package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffCarHistoryResponse;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.StaffCarHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli VIN servis geçmişi. Auth BookingStaffApiController ile aynı. Hyper çağırmaz.
 * en: Partner-panel VIN service history. Auth matches BookingStaffApiController. Does not call Hyper.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffCarHistoryController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final StaffCarHistoryService staffCarHistoryService;

    /**
     * tr: Bu şubedeki visits satırları. Rezerv listesi değil. carId yoksa VIN ile aranır.
     * en: Visits rows at this branch. Not the booking list. Looks up by VIN when carId is absent.
     */
    @GetMapping("/api/v1/booking/staff/cars/history")
    public StaffCarHistoryResponse history(
            HttpServletRequest request,
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) String vin,
            @RequestParam(required = false) Long branchId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return staffCarHistoryService.history(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                carId,
                vin,
                branchId,
                acceptLanguage);
    }
}
