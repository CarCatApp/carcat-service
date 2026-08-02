package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AppointmentResponse;
import com.carland.carland_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * tr: Randevu (appointment) REST controller'ı; tarih bazlı randevu listesini getirme ve resepsiyondan randevu oluşturma uçlarını sunar.
 * en: REST controller for appointments; exposes endpoints to list bookings by date and to create an appointment from the reception.
 */
@RestController
@RequestMapping("/api/v1/appointment")
@RequiredArgsConstructor

public class AppointmentController {
    private final AppointmentService appointmentService;

    /**
     * tr: Verilen tarihe göre randevu listesini döner; role, phoneNumber, X-User-Id, timezone ve Accept-Language header'larını servise iletir.
     * en: Returns the booking list for the given date; forwards the role, phoneNumber, X-User-Id, timezone, and Accept-Language headers to the service layer.
     */
    @GetMapping("/list")
    public List<AppointmentResponse> getBookingListByDate(@RequestParam String date,
                                                          @RequestHeader("role") String role,
                                                          @RequestHeader("phoneNumber") String phoneNumber,
                                                          @RequestHeader("X-User-Id") String userIdHeader,
                                                          @RequestHeader("X-Client-Timezone") String timezone,
                                                          @RequestHeader("Accept-Language") String acceptLanguage) {

        return appointmentService.getBookingListByDate(date, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Resepsiyondan, verilen rangeId'ye ait zaman aralığı için randevu oluşturur; rol ve kullanıcı bilgileri header'lardan alınır, oluşturulan randevuyu döner.
     * en: Creates an appointment from the reception for the time range identified by rangeId; role and user info come from headers, returns the created appointment.
     */
    @PostMapping("/reception")
    public AppointmentResponse setAppointmentFromReception(@RequestParam Long rangeId,
                                                           @RequestHeader("role") String role,
                                                           @RequestHeader("phoneNumber") String phoneNumber,
                                                           @RequestHeader("X-User-Id") String userIdHeader,
                                                           @RequestHeader("X-Client-Timezone") String timezone,
                                                           @RequestHeader("Accept-Language") String acceptLanguage) {

        return appointmentService.setAppointmentFromReception(role, phoneNumber, rangeId, userIdHeader, timezone, acceptLanguage);
    }

//    @GetMapping("/get/byId")
//    public ReceptionAppointmentResponse getAppointmentById(@RequestParam Long appointmentId,
//                                                           @RequestHeader("role") String role,
//                                                           @RequestHeader("phoneNumber") String phoneNumber,
//                                                           @RequestHeader("X-User-Id") String userIdHeader,
//                                                           @RequestHeader("X-Client-Timezone") String timezone,
//                                                           @RequestHeader("Accept-Language") String acceptLanguage) {
//        return appointmentService.getAppointmentById(appointmentId, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
//    }

}
