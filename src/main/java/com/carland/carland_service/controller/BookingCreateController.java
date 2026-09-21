package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingQuoteResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.BookingWriteRequest;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.service.BookingCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Müşteri quote + create (CRCT-284). Flag: booking.
 * en: Owner quote + create (CRCT-284). Flag: booking.
 */
@RestController
@RequiredArgsConstructor
public class BookingCreateController {

    private final BookingCreateService bookingCreateService;

    @PostMapping("/api/v1/booking/bookings/quote")
    public BookingQuoteResponse quote(
            @RequestHeader("Authorization") String token,
            @RequestBody BookingWriteRequest request
    ) {
        return bookingCreateService.quote(request);
    }

    @PostMapping("/api/v1/booking/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView create(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Client-Timezone", required = false) String timezone,
            @RequestBody BookingWriteRequest request
    ) {
        return bookingCreateService.create(request, parseUserId(userIdHeader), timezone);
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
