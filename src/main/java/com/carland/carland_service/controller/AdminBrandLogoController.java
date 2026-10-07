package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandLogoRepository;
import com.carland.carland_service.repository.BrandRepository;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.PhotoService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * tr: Admin panel marka logosu sayfası. Cookie auth. Panel GET {@code /api/v1/photo/for/brand/get/{brandId}}.
 * en: Admin panel brand-logo page. Cookie auth. Panel GET is {@code /api/v1/photo/for/brand/get/{brandId}}.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminBrandLogoController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final BrandRepository brandRepository;
    private final BrandLogoRepository brandLogoRepository;
    private final PhotoService photoService;

    @GetMapping(value = "/admin/brand-logos", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional(readOnly = true)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        List<Brand> brands = brandRepository.findAll();
        brands.sort(Comparator.comparing(Brand::getBrandName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        Set<Long> withLogo = new HashSet<>(brandLogoRepository.findAllBrandIds());
        model.addAttribute("brands", brands);
        model.addAttribute("withLogo", withLogo);
        return "brand-logos";
    }

    @GetMapping("/admin/brand-logos/get")
    public ResponseEntity<byte[]> get(@RequestParam Long brandId, HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return photoService.getBrandLogo(brandId);
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/admin/brand-logos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<PhotoResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long brandId,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoService.uploadBrandLogo(file, brandId));
    }
}
