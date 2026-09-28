package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminServiceBehaviorSaveRequest;
import com.carland.carland_service.dto.response.AdminServiceBehaviorRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.OfferedCatalogService;
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
 * tr: Admin panel xidmət grupları. Cookie auth. Silme yok; pasifleştirme active=false.
 * en: Admin panel service groups. Cookie auth. No delete; deactivate with active=false.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminServiceBehaviorController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final OfferedCatalogService offeredCatalogService;

    /**
     * tr: Grup tablosu.
     * en: Group table.
     */
    @GetMapping(value = "/admin/service-behaviors", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("behaviors", offeredCatalogService.behaviorRows());
        return "service-behaviors";
    }

    /**
     * tr: Grup oluşturur veya günceller. code yalnızca yeni kayıtta yazılır.
     * en: Creates or updates a group. code is written only on create.
     */
    @PostMapping(value = "/admin/service-behaviors/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminServiceBehaviorRow> save(@RequestBody AdminServiceBehaviorSaveRequest body,
                                                        HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(offeredCatalogService.saveBehavior(body));
    }
}
