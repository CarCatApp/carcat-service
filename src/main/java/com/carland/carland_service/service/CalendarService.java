package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.dto.response.CalendarResponse;

/**
 * tr: Oto servislere ait randevu takvimi (calendar) oluşturma ve sorgulama işlemlerinin sözleşmesidir.
 * en: Contract for creating and querying the appointment calendar of auto services.
 */
public interface CalendarService {
    /**
     * tr: Bir oto servis için takvim (gün ve saat aralıkları) oluşturur ve oluşturulan takvimi döner.
     * en: Creates a calendar (days and time ranges) for an auto service and returns the created calendar.
     */
    CalendarResponse createCalendar(CalendarRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Verilen oto servis id'sine ait takvimi rol bilgisine göre döner.
     * en: Returns the calendar for the given auto service id, scoped by role.
     */
    CalendarResponse getCalendarByAutoServiceId(CalendarRequest request, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

}
