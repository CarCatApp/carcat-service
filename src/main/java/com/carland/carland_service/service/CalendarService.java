package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.dto.response.CalendarResponse;

/**
 * tr: Şube randevu takvimi oluşturma ve sorgulama.
 * en: Create and query a branch appointment calendar.
 */
public interface CalendarService {

    CalendarResponse createCalendar(CalendarRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    CalendarResponse getCalendarByBranchId(CalendarRequest request, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);
}
