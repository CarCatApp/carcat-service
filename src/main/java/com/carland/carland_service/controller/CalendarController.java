package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.dto.response.CalendarResponse;
import com.carland.carland_service.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * tr: Takvim REST controller'ı; oto servis için takvim oluşturma ve takvimi getirme uçlarını sunar.
 * en: REST controller for calendars; exposes endpoints to create a calendar and to fetch the calendar for an auto service.
 */
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    /**
     * tr: Gövdedeki CalendarRequest ile yeni bir takvim oluşturur; phoneNumber, X-User-Id, timezone ve Accept-Language header'larını servise iletir ve oluşturulan takvimi döner.
     * en: Creates a new calendar from the CalendarRequest body; forwards the phoneNumber, X-User-Id, timezone, and Accept-Language headers to the service layer and returns the created calendar.
     */
    @PostMapping("/create")
    public CalendarResponse createCalendar(@RequestHeader("Authorization") String token,
                                           @RequestBody CalendarRequest request,
                                           @RequestHeader("phoneNumber") String phoneNumber,
                                           @RequestHeader("X-User-Id") String userIdHeader,
                                           @RequestHeader("X-Client-Timezone") String timezone,
                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        return calendarService.createCalendar(request, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    /**
     * tr: Gövdedeki CalendarRequest'e göre oto servise ait takvimi getirir; rol, phoneNumber ve X-User-Id header'ları ile yetki bağlamı servise iletilir.
     * en: Fetches the calendar belonging to an auto service based on the CalendarRequest body; the role, phoneNumber, and X-User-Id headers convey the authorization context to the service layer.
     */
    @GetMapping("/get")

    public CalendarResponse getCalendarByDoctorId(@RequestHeader("Authorization") String token,
                                                  @RequestBody CalendarRequest request,
                                                  @RequestHeader("X-Client-Timezone") String timezone,
                                                  @RequestHeader("Accept-Language") String acceptLanguage,
                                                  @RequestHeader("role") String role,
                                                  @RequestHeader("phoneNumber") String phoneNumber,
                                                  @RequestHeader("X-User-Id") String userIdHeader
                                                  ) {

        return calendarService.getCalendarByAutoServiceId(request, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }
}
