package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminPromoSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminPromoRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminPromoService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * tr: Promo admin sayfası ve JSON. Cookie auth. Şube id'leri body.branchIds.
 * en: Promo admin page and JSON. Cookie auth. Branch ids are body.branchIds.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminPromoController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final AdminPromoService adminPromoService;

    @GetMapping(value = "/admin/promos", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "promos";
    }

    @GetMapping("/admin/promos/branches")
    @ResponseBody
    public ResponseEntity<List<AdminBannerBranchOption>> branches(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminPromoService.branches());
    }

    @GetMapping("/admin/promos/items")
    @ResponseBody
    public ResponseEntity<List<AdminPromoRow>> items(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminPromoService.all());
    }

    @PostMapping(value = "/admin/promos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminPromoRow> create(@RequestBody AdminPromoSaveRequest body,
                                                HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminPromoService.create(body));
    }

    @PutMapping(value = "/admin/promos/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminPromoRow> update(@PathVariable Long id,
                                                @RequestBody AdminPromoSaveRequest body,
                                                HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminPromoService.update(id, body));
    }
}
