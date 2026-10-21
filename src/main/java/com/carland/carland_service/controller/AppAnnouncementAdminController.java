package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * tr: Duyuru admin HTML sayfası. JSON REST ayrı controller'dadır.
 * en: Announcement admin HTML page. JSON REST lives in a separate controller.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AppAnnouncementAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;

    @GetMapping(value = "/admin/app-announcements", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "app-announcements";
    }
}
