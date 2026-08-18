package com.carland.carland_service.controller;

import com.carland.carland_service.security.AdminAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * tr: Feature-flag admin HTML sayfası. JSON REST ayrı controller'dadır.
 * en: Feature-flag admin HTML page. JSON REST lives in a separate controller.
 */
@Controller
@RequiredArgsConstructor
public class FeatureFlagAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;

    @GetMapping(value = "/admin/feature-flags", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "feature-flags";
    }
}
