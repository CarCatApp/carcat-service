package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminOfferedServiceSaveRequest;
import com.carland.carland_service.dto.response.AdminOfferedServiceRow;
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
 * tr: Admin panel xidmət satırları. Grup listesi sayfa açılışında bir kez yüklenir.
 * en: Admin panel service lines. The group list is loaded once when the page opens.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminOfferedServiceController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final OfferedCatalogService offeredCatalogService;

    /**
     * tr: Xidmət tablosu ve grup seçimi.
     * en: Service table and group picker.
     */
    @GetMapping(value = "/admin/offered-services", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("services", offeredCatalogService.offeredRows());
        model.addAttribute("behaviors", offeredCatalogService.behaviorRows());
        return "offered-services";
    }

    /**
     * tr: Xidmət oluşturur veya günceller. behaviorId zorunludur.
     * en: Creates or updates a service. behaviorId is required.
     */
    @PostMapping(value = "/admin/offered-services/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminOfferedServiceRow> save(@RequestBody AdminOfferedServiceSaveRequest body,
                                                       HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(offeredCatalogService.saveOffered(body));
    }
}
