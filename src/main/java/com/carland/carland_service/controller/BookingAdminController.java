package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffProvisionResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.BookingOrgService;
import com.carland.carland_service.service.BookingStaffAuditService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Hidden
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookingAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final BookingOrgService bookingOrgService;
    private final BookingStaffAuditService staffAuditService;

    @GetMapping(value = "/admin/booking-partners", produces = MediaType.TEXT_HTML_VALUE)
    public String list(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("partners", bookingOrgService.listPartners());
        return "booking-partners";
    }

    @GetMapping(value = "/admin/booking-partners/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        Partner partner = bookingOrgService.getPartner(id);
        model.addAttribute("partner", partner);
        model.addAttribute("branches", bookingOrgService.listBranches(id));
        model.addAttribute("staff", bookingOrgService.listStaff(id));
        model.addAttribute("audit", staffAuditService.listForPartner(id));
        return "booking-partner-detail";
    }

    @PostMapping("/admin/booking-partners")
    public String createPartner(
            @RequestParam String name,
            @RequestParam(required = false) String photo,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String contactEmail,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            Partner created = bookingOrgService.createPartner(
                    name, true, photo, contactPhone, contactEmail);
            return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + created.getId();
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("partnersError", ex.getMessage());
            return "redirect:" + ADMIN_URL + "/admin/booking-partners";
        }
    }

    @PostMapping("/admin/booking-partners/{id}/branches")
    public String addBranch(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String lat,
            @RequestParam(required = false) String lng,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String hoursStart,
            @RequestParam(required = false) String hoursEnd,
            @RequestParam(required = false) String photo,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.addBranch(id, name, address, parseDouble(lat), parseDouble(lng), true,
                    contactPhone, joinWorkingHours(hoursStart, hoursEnd), photo);
            redirect.addFlashAttribute("detailMessage", "Branch əlavə olundu");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @PostMapping("/admin/booking-partners/{id}/staff")
    public String addStaff(
            @PathVariable Long id,
            @RequestParam String role,
            @RequestParam String phoneNumber,
            @RequestParam String email,
            @RequestParam(defaultValue = "SMS") String notifyChannel,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            StaffProvisionResponse created = bookingOrgService.addStaff(
                    id, branchId, role, phoneNumber, name, surname, email, notifyChannel,
                    adminAccessService.actor(request));
            redirect.addFlashAttribute("oneTimePassword", created.getOneTimePassword());
            redirect.addFlashAttribute("oneTimePhone", created.getPhoneNumber());
            String via = "EMAIL".equalsIgnoreCase(notifyChannel) ? "mailə" : "SMS-ə";
            redirect.addFlashAttribute("detailMessage",
                    "Staff yaradıldı — şifrə " + via + " göndərildi, eyni zamanda aşağıda görünür");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    private static String joinWorkingHours(String start, String end) {
        boolean hasStart = start != null && !start.isBlank();
        boolean hasEnd = end != null && !end.isBlank();
        if (!hasStart && !hasEnd) {
            return null;
        }
        if (!hasStart || !hasEnd) {
            return null;
        }
        String from = normalizeClock(start);
        String to = normalizeClock(end);
        if (from == null || to == null) {
            return null;
        }
        return from + "-" + to;
    }

    private static String normalizeClock(String raw) {
        String value = raw.trim();
        if (value.length() >= 5) {
            return value.substring(0, 5);
        }
        return null;
    }

    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
