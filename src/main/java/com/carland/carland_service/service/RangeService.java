package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.RangeResponse;

/**
 * tr: Takvimdeki zaman aralıkları (range) üzerinden randevu alma, onaylama/reddetme ve iptal işlemlerinin sözleşmesidir.
 * en: Contract for booking, approving/rejecting, and cancelling appointments over calendar time ranges.
 */
public interface RangeService {
    /**
     * tr: Müşteri için verilen zaman aralığına randevu talebi oluşturur ve aralığın güncel durumunu döner.
     * en: Creates a booking request for the given time range on behalf of the customer and returns the updated range state.
     */
    RangeResponse bookAppointment(Long rangeId,  String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Servis tarafının randevu talebini kabul veya reddetmesini işler ve aralığın güncel durumunu döner.
     * en: Processes the service side's accept/reject decision on a booking request and returns the updated range state.
     */
    RangeResponse decideOnBooking(Long rangeId, boolean accepted, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Müşterinin kendi randevusunu iptal etmesini sağlar ve aralığın güncel durumunu döner.
     * en: Lets the customer cancel their own booking and returns the updated range state.
     */
    RangeResponse deleteBookingByCustomer(Long rangeId, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);




}
