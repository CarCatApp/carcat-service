package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminFuelIntervalAddRequest;
import com.carland.carland_service.dto.request.AdminFuelIntervalRemoveRequest;
import com.carland.carland_service.dto.request.AdminFuelIntervalSaveRequest;
import com.carland.carland_service.dto.response.AdminFuelIntervalRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminFuelIntervalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * tr: Admin yakıt aralığı səhifəsi. Cookie auth. Mobil uç yoxdur.
 * en: Admin fuel-interval page. Cookie auth. No mobile API.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminFuelIntervalController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final AdminFuelIntervalService adminFuelIntervalService;

    @GetMapping(value = "/admin/fuel-intervals", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@RequestParam(required = false) Long engineTypeId,
                       HttpServletRequest request,
                       Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("screen", adminFuelIntervalService.open(engineTypeId));
        return "fuel-intervals";
    }

    @PostMapping(value = "/admin/fuel-intervals/interval", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminFuelIntervalRow> updateInterval(@RequestBody AdminFuelIntervalSaveRequest body,
                                                               HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminFuelIntervalService.updateInterval(body));
    }

    @PostMapping(value = "/admin/fuel-intervals/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminFuelIntervalRow> add(@RequestBody AdminFuelIntervalAddRequest body,
                                                    HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminFuelIntervalService.add(body));
    }

    @PostMapping(value = "/admin/fuel-intervals/remove", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> remove(@RequestBody AdminFuelIntervalRemoveRequest body,
                                       HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        adminFuelIntervalService.remove(body == null ? null : body.getServiceId());
        return ResponseEntity.noContent().build();
    }
}
