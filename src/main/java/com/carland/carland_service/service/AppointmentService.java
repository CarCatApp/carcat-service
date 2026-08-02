package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.AppointmentResponse;

import java.util.List;

/**
 * tr: Servis randevularıyla ilgili işlemlerin (tarihe göre randevu listesi, resepsiyondan randevu oluşturma) servis sözleşmesidir.
 * en: Service contract for appointment operations (listing bookings by date, creating an appointment from the reception desk).
 */
public interface AppointmentService {
    /**
     * tr: Verilen tarihteki randevu (booking) listesini rol/kullanıcı bilgisine göre döner.
     * en: Returns the list of bookings for the given date, scoped by role/user info.
     */
    List<AppointmentResponse> getBookingListByDate(String date, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Resepsiyon tarafından verilen zaman aralığı (rangeId) için randevu oluşturur ve sonucu döner.
     * en: Creates an appointment from the reception desk for the given time range (rangeId) and returns the result.
     */
    AppointmentResponse setAppointmentFromReception(String role, String phoneNumber, Long rangeId, String userIdHeader, String timezone, String acceptLanguage);




}
