package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.AdminBookingBoardPage;
import com.carland.carland_service.dto.booking.AdminBookingMessageView;
import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.StaffCarHistoryResponse;
import com.carland.carland_service.dto.booking.StaffCompletionDraftView;
import com.carland.carland_service.dto.booking.StaffNotesResponse;
import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminBookingBoardService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * tr: Admin rezerv tahtası. Bütün aktiv şubeler, şube seçimi yok.
 * en: Admin booking board. Every active branch, no branch picker.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminBookingBoardController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final AdminBookingBoardService boardService;

    @GetMapping(value = "/admin/booking-board", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "booking-board";
    }

    @GetMapping(value = "/admin/booking-board/data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AdminBookingBoardPage data(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String vin,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        requireAdmin(request);
        return boardService.page(status, page, vin, plate, firstName, lastName, phone,
                day(dateFrom), day(dateTo));
    }

    @GetMapping(value = "/admin/booking-board/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public StaffNotesResponse notes(HttpServletRequest request, @RequestParam String kind) {
        requireAdmin(request);
        return boardService.notes(kind);
    }

    @PostMapping(value = "/admin/booking-board/{id:\\d+}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BookingView accept(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        return boardService.accept(id);
    }

    @PostMapping(value = "/admin/booking-board/{id:\\d+}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BookingView reject(HttpServletRequest request, @PathVariable Long id,
                              @RequestBody(required = false) BookingRejectRequest body) {
        requireAdmin(request);
        return boardService.reject(id, body);
    }

    @PostMapping(value = "/admin/booking-board/{id:\\d+}/no-show", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BookingView noShow(HttpServletRequest request, @PathVariable Long id,
                              @RequestBody(required = false) BookingRejectRequest body) {
        requireAdmin(request);
        return boardService.noShow(id, body);
    }

    @PostMapping(value = "/admin/booking-board/{id:\\d+}/hide", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BookingView hide(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        return boardService.hide(id);
    }

    @GetMapping(value = "/admin/booking-board/{id:\\d+}/draft", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public StaffCompletionDraftView draft(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        return boardService.draft(id);
    }

    @PostMapping(value = "/admin/booking-board/{id:\\d+}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BookingView complete(HttpServletRequest request, @PathVariable Long id,
                                @RequestBody(required = false) StaffCompleteRequest body) {
        requireAdmin(request);
        return boardService.complete(id, body);
    }

    @GetMapping(value = "/admin/booking-board/{id:\\d+}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public StaffCarHistoryResponse history(HttpServletRequest request, @PathVariable Long id,
                                           @RequestParam(required = false) Long carId,
                                           @RequestParam(required = false) String vin) {
        requireAdmin(request);
        return boardService.history(id, carId, vin);
    }

    @GetMapping(value = "/admin/booking-board/{id:\\d+}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<AdminBookingMessageView> messages(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        return boardService.messages(id);
    }

    @PostMapping(value = "/admin/booking-board/{id:\\d+}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AdminBookingMessageView send(HttpServletRequest request, @PathVariable Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        requireAdmin(request);
        return boardService.send(id, body == null ? null : body.get("body"));
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            throw new ForbiddenException("Admin session required");
        }
    }

    private static LocalDate day(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new MissingFieldException("date is invalid");
        }
    }
}
