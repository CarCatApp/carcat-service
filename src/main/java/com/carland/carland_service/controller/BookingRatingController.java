package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingRatingCreateRequest;
import com.carland.carland_service.dto.booking.BookingRatingView;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.service.BookingRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Müşteri şube yorumu. Score gönüllü (1–5). Flag: booking.
 * en: Owner branch review. Score optional (1–5). Flag: booking.
 */
@RestController
@RequiredArgsConstructor
public class BookingRatingController {

    private final BookingRatingService bookingRatingService;

    @PostMapping("/api/v1/booking/branches/{branchId}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingRatingView create(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable Long branchId,
            @RequestBody(required = false) BookingRatingCreateRequest request
    ) {
        return bookingRatingService.add(branchId, parseUserId(userIdHeader), request);
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
