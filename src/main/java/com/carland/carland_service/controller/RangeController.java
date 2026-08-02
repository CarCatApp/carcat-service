package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.RangeResponse;
import com.carland.carland_service.service.RangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * tr: Zaman aralığı (range) REST controller'ı; müşterinin randevu alması, işletmenin randevu talebini onaylaması/reddetmesi ve müşterinin randevuyu iptal etmesi uçlarını sunar.
 * en: REST controller for time ranges; exposes endpoints for a customer to book an appointment, for the business to accept/reject a booking request, and for the customer to cancel a booking.
 */
@RestController
@RequestMapping("/api/v1/range")
@RequiredArgsConstructor
public class RangeController {

    private final RangeService rangeService;

    /**
     * tr: Verilen rangeId'ye ait zaman aralığı için çağıran kullanıcı adına randevu talebi oluşturur ve sonucu döner.
     * en: Creates a booking request on behalf of the calling user for the time range identified by rangeId and returns the result.
     */
    @PostMapping("/book")
    public RangeResponse bookAppointment(@RequestParam Long rangeId,
                                         @RequestHeader("role") String role,
                                         @RequestHeader("phoneNumber") String phoneNumber,
                                         @RequestHeader("X-User-Id") String userIdHeader,
                                         @RequestHeader("X-Client-Timezone") String timezone,
                                         @RequestHeader("Accept-Language") String acceptLanguage) {
        return rangeService.bookAppointment(rangeId, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    /**
     * tr: Verilen rangeId'ye ait randevu talebini accepted parametresine göre onaylar veya reddeder ve güncellenmiş durumu döner.
     * en: Accepts or rejects the booking request for the given rangeId based on the accepted parameter and returns the updated state.
     */
    @PostMapping("/booking/decision")
    public RangeResponse decideOnBooking(@RequestParam Long rangeId,
                                         @RequestParam boolean accepted,
                                         @RequestHeader("role") String role,
                                         @RequestHeader("phoneNumber") String phoneNumber,
                                         @RequestHeader("X-User-Id") String userIdHeader,
                                         @RequestHeader("X-Client-Timezone") String timezone,
                                         @RequestHeader("Accept-Language") String acceptLanguage) {
        return rangeService.decideOnBooking(rangeId, accepted, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Müşterinin verilen rangeId'ye ait randevusunu iptal eder (siler) ve sonucu döner.
     * en: Cancels (deletes) the customer's booking for the given rangeId and returns the result.
     */
    @PutMapping("/delete")
    public RangeResponse deleteBookingByCustomer(@RequestParam Long rangeId,
                                                @RequestHeader("role") String role,
                                                @RequestHeader("phoneNumber") String phoneNumber,
                                                @RequestHeader("X-User-Id") String userIdHeader,
                                                @RequestHeader("X-Client-Timezone") String timezone,
                                                @RequestHeader("Accept-Language") String acceptLanguage) {
        return rangeService.deleteBookingByCustomer(rangeId, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


}
