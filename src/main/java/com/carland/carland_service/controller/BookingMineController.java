package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingDetailResponse;
import com.carland.carland_service.dto.booking.BookingMineResponse;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.service.BookingMineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Owner rezervasyon listesi + detay (CRCT-285). Flag: booking.
 * en: Owner booking list + detail (CRCT-285). Flag: booking.
 */
@RestController
@RequiredArgsConstructor
public class BookingMineController {

    private final BookingMineService bookingMineService;

    @GetMapping({"/api/v1/booking/bookings", "/api/v1/booking/bookings/mine"})
    public BookingMineResponse mine(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit
    ) {
        return bookingMineService.mine(parseUserId(userIdHeader), status, carId, page, pageSize, limit, timezone);
    }

    @GetMapping("/api/v1/booking/bookings/{bookingId}")
    public BookingDetailResponse detail(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @PathVariable String bookingId
    ) {
        return bookingMineService.detail(parseUserId(userIdHeader), bookingId, timezone);
    }

    private static Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("X-User-Id");
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw MissingFieldException.required("X-User-Id");
        }
    }
}
