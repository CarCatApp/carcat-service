package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminBookingNoteSaveRequest;
import com.carland.carland_service.dto.response.AdminBookingNoteRow;
import com.carland.carland_service.entity.BookingStaffNote;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.BookingStaffNoteService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * tr: Admin Notes: Cancel və No show. Cookie auth. Silme hard delete.
 * en: Admin Notes: Cancel and No show. Cookie auth. Delete removes the row.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminBookingNoteController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final BookingStaffNoteService bookingStaffNoteService;

    @GetMapping(value = "/admin/booking-notes", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("cancelNotes", bookingStaffNoteService.rows(BookingStaffNote.CANCEL));
        model.addAttribute("noShowNotes", bookingStaffNoteService.rows(BookingStaffNote.NO_SHOW));
        return "booking-notes";
    }

    @PostMapping(value = "/admin/booking-notes/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminBookingNoteRow> save(@RequestBody AdminBookingNoteSaveRequest body,
                                                    HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bookingStaffNoteService.save(body));
    }

    @PostMapping(value = "/admin/booking-notes/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> delete(@RequestBody AdminBookingNoteSaveRequest body,
                                       HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        bookingStaffNoteService.delete(body == null ? null : body.getId());
        return ResponseEntity.noContent().build();
    }
}
