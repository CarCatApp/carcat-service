package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.StaffProvisionResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.BookingOrgService;
import com.carland.carland_service.service.BookingStaffAuditService;
import com.carland.carland_service.service.BranchWorkingHours;
import com.carland.carland_service.service.PhotoService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
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
    private final PhotoService photoService;

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
            @RequestParam(required = false) String weekdayStart,
            @RequestParam(required = false) String weekdayEnd,
            @RequestParam(required = false) String saturdayStart,
            @RequestParam(required = false) String saturdayEnd,
            @RequestParam(required = false) String sundayStart,
            @RequestParam(required = false) String sundayEnd,
            @RequestParam(required = false) String verified,
            @RequestParam(required = false) String photo,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.addBranch(id, name, address, parseDouble(lat), parseDouble(lng), true,
                    contactPhone,
                    BranchWorkingHours.range(weekdayStart, weekdayEnd, "weekday"),
                    BranchWorkingHours.range(saturdayStart, saturdayEnd, "saturday"),
                    BranchWorkingHours.range(sundayStart, sundayEnd, "sunday"),
                    "true".equals(verified), photo);
            redirect.addFlashAttribute("detailMessage", "Branch əlavə olundu");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @PostMapping("/admin/booking-partners/{id}/branches/{branchId}")
    public String updateBranch(
            @PathVariable Long id,
            @PathVariable Long branchId,
            @RequestParam(required = false) String weekdayStart,
            @RequestParam(required = false) String weekdayEnd,
            @RequestParam(required = false) String saturdayStart,
            @RequestParam(required = false) String saturdayEnd,
            @RequestParam(required = false) String sundayStart,
            @RequestParam(required = false) String sundayEnd,
            @RequestParam(required = false) String verified,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String lat,
            @RequestParam(required = false) String lng,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.updateBranchHours(id, branchId, "true".equals(verified),
                    BranchWorkingHours.range(weekdayStart, weekdayEnd, "weekday"),
                    BranchWorkingHours.range(saturdayStart, saturdayEnd, "saturday"),
                    BranchWorkingHours.range(sundayStart, sundayEnd, "sunday"),
                    name, address, contactPhone, parseCoordinate(lat, "Lat"), parseCoordinate(lng, "Lng"));
            redirect.addFlashAttribute("detailMessage", "Şöbə yeniləndi");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @PostMapping("/admin/booking-partners/{id}/active")
    public String setPartnerActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.setPartnerActive(id, active);
            redirect.addFlashAttribute("detailMessage", active ? "Partner aktiv edildi" : "Partner deaktiv edildi");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @PostMapping("/admin/booking-partners/{id}/branches/{branchId}/active")
    public String setBranchActive(
            @PathVariable Long id,
            @PathVariable Long branchId,
            @RequestParam boolean active,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.setBranchActive(id, branchId, active);
            redirect.addFlashAttribute("detailMessage", active ? "Şöbə aktiv edildi" : "Şöbə deaktiv edildi");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @PostMapping("/admin/booking-partners/{id}/branches/{branchId}/completion")
    public String setBranchCompletion(
            @PathVariable Long id,
            @PathVariable Long branchId,
            @RequestParam(required = false) String detailed,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            boolean panel = "true".equals(detailed);
            bookingOrgService.setBranchCompletion(id, branchId, panel);
            redirect.addFlashAttribute("detailMessage",
                    panel ? "Şöbə detailed tamamlama rejiminə keçdi" : "Şöbə webhook rejiminə keçdi");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @GetMapping("/admin/booking-partners/{id}/branches/{branchId}/badge")
    public ResponseEntity<byte[]> branchBadge(
            @PathVariable Long id,
            @PathVariable Long branchId,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            bookingOrgService.requireBranch(id, branchId);
            ResponseEntity<byte[]> logo = photoService.getBranchBadgeLogoById(branchId);
            return ResponseEntity.status(logo.getStatusCode())
                    .headers(logo.getHeaders())
                    .cacheControl(CacheControl.noStore())
                    .body(logo.getBody());
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/admin/booking-partners/{id}/branches/{branchId}/badge",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadBranchBadge(
            @PathVariable Long id,
            @PathVariable Long branchId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.requireBranch(id, branchId);
            photoService.uploadBranchBadgeLogo(file, branchId);
            redirect.addFlashAttribute("detailMessage", "Badge yükləndi");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("detailError", ex.getMessage());
        }
        return "redirect:" + ADMIN_URL + "/admin/booking-partners/" + id;
    }

    @PostMapping("/admin/booking-partners/{id}/staff/{staffId}/manual")
    public String setStaffManual(
            @PathVariable Long id,
            @PathVariable Long staffId,
            @RequestParam boolean disabled,
            HttpServletRequest request,
            RedirectAttributes redirect
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        try {
            bookingOrgService.setStaffManual(id, staffId, disabled);
            redirect.addFlashAttribute("detailMessage", disabled ? "Staff deaktiv edildi" : "Staff aktiv edildi");
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

    private static Double parseCoordinate(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new MissingFieldException(label + " rəqəm olmalıdır");
        }
    }
}
