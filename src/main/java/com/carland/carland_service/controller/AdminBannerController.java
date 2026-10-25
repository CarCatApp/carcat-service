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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/admin/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<AdminBannerRow> createMultipart(@RequestParam String title,
                                                         @RequestParam String description,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) Long branchId,
                                                         @RequestParam(required = false) String url,
                                                         @RequestParam(required = false) Integer sortOrder,
                                                         @RequestParam(required = false) MultipartFile file,
                                                         HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AdminBannerSaveRequest body = new AdminBannerSaveRequest();
        body.setTitle(title);
        body.setDescription(description);
        body.setType(type);
        body.setBranchId(branchId);
        body.setUrl(url);
        body.setSortOrder(sortOrder);
        return ResponseEntity.ok(adminBannerService.create(body, file));
    }

    @PostMapping(value = "/admin/banners/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<AdminBannerRow> replacePhoto(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(adminBannerService.replacePhoto(id, file));
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
