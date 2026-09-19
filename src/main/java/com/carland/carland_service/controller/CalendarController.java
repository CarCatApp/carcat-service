package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.dto.response.CalendarResponse;
import com.carland.carland_service.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * tr: Şube randevu takvimi oluşturma ve getirme.
 * en: Create and fetch a branch appointment calendar.
 */
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    @PostMapping("/create")
    public CalendarResponse createCalendar(@RequestHeader("Authorization") String token,
                                           @RequestBody CalendarRequest request,
                                           @RequestHeader("phoneNumber") String phoneNumber,
                                           @RequestHeader("X-User-Id") String userIdHeader,
                                           @RequestHeader("X-Client-Timezone") String timezone,
                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        return calendarService.createCalendar(request, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    @GetMapping("/get")
    public CalendarResponse getCalendarByBranchId(@RequestHeader("Authorization") String token,
                                                  @RequestBody CalendarRequest request,
                                                  @RequestHeader("X-Client-Timezone") String timezone,
                                                  @RequestHeader("Accept-Language") String acceptLanguage,
                                                  @RequestHeader("role") String role,
                                                  @RequestHeader("phoneNumber") String phoneNumber,
                                                  @RequestHeader("X-User-Id") String userIdHeader) {
        return calendarService.getCalendarByBranchId(request, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }
}
