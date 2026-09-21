package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingAvailabilityResponse;
import com.carland.carland_service.dto.booking.BookingCatalogResponse;
import com.carland.carland_service.dto.booking.BookingDiscoveryResponse;
import com.carland.carland_service.service.BookingAvailabilityService;
import com.carland.carland_service.service.BookingCatalogService;
import com.carland.carland_service.service.BookingDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Müşteri keşif + katalog + müsaitlik. Flag: booking.
 * en: Owner discovery + catalog + availability. Flag: booking.
 */
@RestController
@RequiredArgsConstructor
public class BookingDiscoveryController {

    private final BookingDiscoveryService bookingDiscoveryService;
    private final BookingCatalogService bookingCatalogService;
    private final BookingAvailabilityService bookingAvailabilityService;

    @GetMapping("/api/v1/booking/partners")
    public BookingDiscoveryResponse discover(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return bookingDiscoveryService.discover(partnerId, q, lat, lng, radius, page, pageSize);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/catalog")
    public BookingCatalogResponse catalog(
            @RequestHeader("Authorization") String token,
            @PathVariable Long branchId
    ) {
        return bookingCatalogService.catalog(branchId);
    }

    @GetMapping("/api/v1/booking/branches/{branchId}/availability")
    public BookingAvailabilityResponse availability(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @PathVariable Long branchId,
            @RequestParam(required = false) String serviceKeys,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return bookingAvailabilityService.availability(branchId, serviceKeys, from, to, timezone);
    }
}
