package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminBrandModelHeadingSaveRequest;
import com.carland.carland_service.dto.response.AdminBrandModelHeadingRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.BrandModelHeadingService;
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
 * tr: Admin marka başlıkları. Şube bu başlığın id'sine ürün yazar.
 * en: Admin brand headings. A branch adds products under the heading id.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminBrandModelController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final BrandModelHeadingService brandModelHeadingService;

    @GetMapping(value = "/admin/brand-models", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("headings", brandModelHeadingService.rows());
        return "brand-models";
    }

    @PostMapping(value = "/admin/brand-models/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminBrandModelHeadingRow> save(@RequestBody AdminBrandModelHeadingSaveRequest body,
                                                          HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(brandModelHeadingService.save(body));
    }

    @PostMapping("/admin/brand-models/delete")
    @ResponseBody
    public ResponseEntity<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        brandModelHeadingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
