package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminIndividualServiceSaveRequest;
import com.carland.carland_service.dto.response.AdminIndividualServiceRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.IndividualCatalogService;
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
 * tr: Admin fərdi xidmət kataloqu. Şube fiyatı bu sayfada yok.
 * en: Admin individual-service catalog. Branch prices are not on this page.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminIndividualServiceController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final IndividualCatalogService individualCatalogService;

    @GetMapping(value = "/admin/individual-services", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("services", individualCatalogService.adminRows());
        return "individual-services";
    }

    @PostMapping(value = "/admin/individual-services/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminIndividualServiceRow> save(@RequestBody AdminIndividualServiceSaveRequest body,
                                                          HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(individualCatalogService.saveAdmin(body));
    }
}
