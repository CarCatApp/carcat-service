package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingCatalogResponse;
import com.carland.carland_service.dto.booking.BookingDiscoveryResponse;
import com.carland.carland_service.service.BookingCatalogService;
import com.carland.carland_service.service.BookingDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Müşteri şube keşfi + katalog (CRCT-281/282). Flag: booking.
 * en: Owner-app discovery + catalog (CRCT-281/282). Flag: booking.
 */
@RestController
@RequiredArgsConstructor
public class BookingDiscoveryController {

    private final BookingDiscoveryService bookingDiscoveryService;
    private final BookingCatalogService bookingCatalogService;

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
}
