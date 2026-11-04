package com.carland.carland_service.controller;

import com.carland.carland_service.dto.commission.CommissionDetail;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.commission.CommissionExcelWriter;
import com.carland.carland_service.service.commission.CommissionReportService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;

/**
 * tr: Admin komisyon səhifəsi. Cookie ilə giriş. Mobil uç yoxdur.
 * en: Admin commission page. Cookie login. No mobile API.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class CommissionAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final CommissionReportService report;
    private final CommissionExcelWriter excel;

    @GetMapping("/admin/commissions")
    public String page(@RequestParam(required = false) String preset,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String partnerId,
                       @RequestParam(required = false) String branchId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir,
                       @RequestParam(defaultValue = "1") int page,
                       HttpServletRequest request,
                       Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("screen", report.screen(preset, from, to, id(partnerId), id(branchId), status, q, sort, dir, page));
        return "commissions";
    }

    @GetMapping("/admin/commissions/{id:\\d+}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        CommissionDetail detail = report.detail(id);
        if (detail == null) {
            return "redirect:" + ADMIN_URL + "/admin/commissions";
        }
        model.addAttribute("detail", detail);
        return "commission-detail";
    }

    @GetMapping("/admin/commissions/export")
    public void export(@RequestParam(required = false) String preset,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String partnerId,
                       @RequestParam(required = false) String branchId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        if (!adminAccessService.isPanelAdmin(request)) {
            response.sendRedirect(ADMIN_URL + "/admin/");
            return;
        }
        excel.write(report.export(preset, from, to, id(partnerId), id(branchId), status, q), response);
    }

    @PostMapping("/admin/commissions/rates")
    public String addRate(@RequestParam(required = false) String partnerId,
                          @RequestParam(required = false) String customerStatus,
                          @RequestParam(required = false) String percent,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
                          @RequestParam(required = false) String note,
                          @RequestParam(required = false) String preset,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(required = false) String filterPartnerId,
                          @RequestParam(required = false) String branchId,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String q,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        String error = report.addRate(id(partnerId), customerStatus, percent, effectiveFrom, note);
        if (error == null) {
            redirectAttributes.addFlashAttribute("rateMessage", "Faiz qeyd olundu. Köhnə sətirlər dəyişmədi.");
        } else {
            redirectAttributes.addFlashAttribute("rateError", error);
        }
        UriComponentsBuilder back = UriComponentsBuilder.fromPath("/admin/commissions");
        if (preset != null && !preset.isBlank()) {
            back.queryParam("preset", preset);
        }
        if (from != null) {
            back.queryParam("from", from);
        }
        if (to != null) {
            back.queryParam("to", to);
        }
        if (filterPartnerId != null && !filterPartnerId.isBlank()) {
            back.queryParam("partnerId", filterPartnerId);
        }
        if (branchId != null && !branchId.isBlank()) {
            back.queryParam("branchId", branchId);
        }
        if (status != null && !status.isBlank()) {
            back.queryParam("status", status);
        }
        if (q != null && !q.isBlank()) {
            back.queryParam("q", q);
        }
        return "redirect:" + ADMIN_URL + back.build().encode().toUriString();
    }

    private static Long id(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
