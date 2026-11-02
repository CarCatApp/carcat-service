package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.clientevent.ClientEventAdminService;
import com.carland.carland_service.service.clientevent.ClientEventAdminService.DetailView;
import com.carland.carland_service.service.clientevent.ClientEventAdminService.IssueEventsView;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * tr: Admin xəta ekranları. Mətn Azərbaycan dilində, Thymeleaf.
 * en: Admin error screens. Copy is Azerbaijani, rendered with Thymeleaf.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class ClientEventAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final ClientEventAdminService adminService;

    @GetMapping("/admin/client-events")
    public String issues(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("view", adminService.issues(type, from, to, page));
        return "client-events";
    }

    @GetMapping("/admin/client-events/issues/{id:\\d+}")
    public String issue(
            @PathVariable long id,
            @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        IssueEventsView view = adminService.issueEvents(id, page);
        if (view == null) {
            return "redirect:/admin/client-events";
        }
        model.addAttribute("view", view);
        return "client-event-issue";
    }

    @GetMapping("/admin/users/{userId:\\d+}/client-events")
    public String user(
            @PathVariable long userId,
            @RequestParam(defaultValue = "overview") String tab,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("view", adminService.user(userId, tab, from, to, page));
        return "client-event-user";
    }

    @GetMapping("/admin/client-events/events/{id:\\d+}")
    public String detail(
            @PathVariable long id,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) Long issueId,
            HttpServletRequest request,
            Model model
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        DetailView view = adminService.detail(id);
        model.addAttribute("view", view);
        model.addAttribute("backUrl", backUrl(view, userId, tab, issueId));
        return "client-event-detail";
    }

    private static String backUrl(DetailView view, Long userId, String tab, Long issueId) {
        if (userId != null) {
            return "/admin/users/" + userId + "/client-events?tab=" + safeTab(tab);
        }
        if (issueId != null) {
            return "/admin/client-events/issues/" + issueId;
        }
        if (view != null && view.userId() != null) {
            return "/admin/users/" + view.userId() + "/client-events?tab=timeline";
        }
        if (view != null) {
            return "/admin/client-events/issues/" + view.issueId();
        }
        return "/admin/client-events";
    }

    private static String safeTab(String tab) {
        return switch (tab == null ? "" : tab) {
            case "fatal", "server", "client", "nonfatal", "device", "timeline", "overview" -> tab;
            default -> "overview";
        };
    }
}
