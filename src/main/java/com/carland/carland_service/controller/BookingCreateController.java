package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingAppointmentCancelRequest;
import com.carland.carland_service.dto.booking.BookingAppointmentCancelResponse;
import com.carland.carland_service.dto.booking.BookingAppointmentRequest;
import com.carland.carland_service.dto.booking.BookingAppointmentResponse;
import com.carland.carland_service.dto.booking.BookingQuoteResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.BookingWriteRequest;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.service.BookingAppointmentService;
import com.carland.carland_service.service.BookingCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final BookingAppointmentService bookingAppointmentService;

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
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestBody BookingWriteRequest request
    ) {
        return bookingCreateService.create(request, parseUserId(userIdHeader), timezone, acceptLanguage);
    }

    /**
     * tr: Seçilen range'e book. Paket, xidmət və şikayət bu range'ə yazılır.
     * en: Books the chosen range. The package, services and complaint are stored on that range.
     */
    @PostMapping("/api/v1/booking/branches/{branchId}/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingAppointmentResponse appoint(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long branchId,
            @RequestBody BookingAppointmentRequest request
    ) {
        return bookingAppointmentService.create(branchId, request, parseUserId(userIdHeader), acceptLanguage);
    }

    /**
     * tr: Müştəri öz bookunu ləğv edir. Səbəb bookingə yazılır.
     * en: The customer cancels their own booking. The reason is stored on the booking.
     */
    @PostMapping("/api/v1/booking/appointments/{bookingId}/cancel")
    public BookingAppointmentCancelResponse cancel(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PathVariable Long bookingId,
            @RequestBody BookingAppointmentCancelRequest request
    ) {
        return bookingAppointmentService.cancel(bookingId, request, parseUserId(userIdHeader), acceptLanguage);
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
