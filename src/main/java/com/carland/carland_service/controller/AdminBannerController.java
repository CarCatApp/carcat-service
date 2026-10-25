package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminBannerSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminBannerRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminBannerService;
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
 * tr: Banner admin sayfası ve JSON. Cookie auth. Mobil uç yok.
 * en: Banner admin page and JSON. Cookie auth. No mobile API.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminBannerController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final AdminBannerService adminBannerService;

    @GetMapping(value = "/admin/banners", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "banners";
    }

    @GetMapping("/admin/banners/branches")
    @ResponseBody
    public ResponseEntity<List<AdminBannerBranchOption>> branches(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminBannerService.branches());
    }

    @GetMapping("/admin/banners/items")
    @ResponseBody
    public ResponseEntity<List<AdminBannerRow>> items(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminBannerService.active());
    }

    @PostMapping(value = "/admin/banners", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminBannerRow> create(@RequestBody AdminBannerSaveRequest body,
                                                 HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminBannerService.create(body));
    }

    @PutMapping(value = "/admin/banners/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminBannerRow> update(@PathVariable Long id,
                                                 @RequestBody AdminBannerSaveRequest body,
                                                 HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminBannerService.update(id, body));
    }
}
