package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffSlotCoverageResponse;
import com.carland.carland_service.dto.booking.StaffSlotDayResponse;
import com.carland.carland_service.dto.booking.StaffSlotGenerateResponse;
import com.carland.carland_service.dto.request.StaffSlotCapacityRequest;
import com.carland.carland_service.dto.request.StaffSlotDayHiddenRequest;
import com.carland.carland_service.dto.request.StaffSlotGenerateRequest;
import com.carland.carland_service.dto.request.StaffSlotHiddenRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.StaffSlotEditService;
import com.carland.carland_service.service.StaffSlotGenerateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner paneli slot üretimi ve gün listesi.
 * en: Partner-panel slot generation and day list.
 */
@RestController
@RequiredArgsConstructor
public class BookingStaffSlotController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final StaffSlotGenerateService staffSlotGenerateService;
    private final StaffSlotEditService staffSlotEditService;

    /**
     * tr: Seçilen gün aralığında saat dilimlerini yazar. Dolu gün o hedef için atlanır.
     * en: Writes time slices for the selected dates. An occupied day is skipped for that target.
     */
    @PostMapping("/api/v1/booking/staff/slots/generate")
    public StaffSlotGenerateResponse generate(
            HttpServletRequest request,
            @RequestBody StaffSlotGenerateRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffSlotGenerateService.generate(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }

    /**
     * tr: Şubenin bir günündeki slotlar.
     * en: Slots of the branch for one day.
     */
    @GetMapping("/api/v1/booking/staff/slots")
    public StaffSlotDayResponse day(
            HttpServletRequest request,
            @RequestParam String day,
            @RequestParam(required = false) Long branchId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffSlotGenerateService.day(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                branchId,
                day,
                acceptLanguage);
    }

    /**
     * tr: Bugünden itibaren saati olan günler. Boş günler yok.
     * en: Days from today that have a slot. Empty days are omitted.
     */
    @GetMapping("/api/v1/booking/staff/slots/coverage")
    public StaffSlotCoverageResponse coverage(
            HttpServletRequest request,
            @RequestParam(required = false) Long branchId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffSlotGenerateService.coverage(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                branchId,
                acceptLanguage);
    }

    /**
     * tr: Saatin yer sayısını bir artırır veya azaltır.
     * en: Raises or lowers the hour's place count by one.
     */
    @PostMapping("/api/v1/booking/staff/slots/{slotId}/capacity")
    public void capacity(
            HttpServletRequest request,
            @PathVariable Long slotId,
            @RequestBody StaffSlotCapacityRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        staffSlotEditService.capacity(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                slotId,
                body,
                acceptLanguage);
    }

    /**
     * tr: Saati gizlər veya açar. Satır silinmez.
     * en: Hides or reopens the hour. The row is not deleted.
     */
    @PostMapping("/api/v1/booking/staff/slots/{slotId}/hidden")
    public void hidden(
            HttpServletRequest request,
            @PathVariable Long slotId,
            @RequestBody StaffSlotHiddenRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        staffSlotEditService.hidden(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                slotId,
                body,
                acceptLanguage);
    }

    /**
     * tr: Seçili hizmetin gününü gizlər veya açar. Diğer hizmetler durur.
     * en: Hides or reopens the selected service's day. Other services stay.
     */
    @PostMapping("/api/v1/booking/staff/slots/day-hidden")
    public void dayHidden(
            HttpServletRequest request,
            @RequestBody StaffSlotDayHiddenRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        staffSlotEditService.dayHidden(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                body,
                acceptLanguage);
    }
}
