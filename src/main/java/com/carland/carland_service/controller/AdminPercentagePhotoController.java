package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
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
import java.util.List;

/**
 * tr: Admin panel bakım-kalemi ikon sayfası; cookie auth. Mobil API {@code PhotoController} üzerindedir.
 * en: Admin panel maintenance-item icon page; cookie auth. Mobile API lives on {@code PhotoController}.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminPercentagePhotoController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final PhotoService photoService;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;

    @GetMapping(value = "/admin/percentage-photos", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional(readOnly = true)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        List<MaintenanceTemplate> templates = maintenanceTemplateRepository.findAllWithEngineAndServices();
        templates.sort(Comparator.comparing(MaintenanceTemplate::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        for (MaintenanceTemplate template : templates) {
            if (template.getServices() == null) {
                continue;
            }
            template.getServices().sort(Comparator
                    .comparing(ServiceEntity::getNameEn, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(ServiceEntity::getId, Comparator.nullsLast(Long::compareTo)));
        }
        model.addAttribute("templates", templates);
        return "percentage-photos";
    }

    @GetMapping("/admin/percentage-photos/get")
    public ResponseEntity<byte[]> getServicePhoto(
            @RequestParam Long serviceId,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return photoService.getPercentagePhoto(serviceId);
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/admin/percentage-photos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<PhotoResponse> uploadServicePhoto(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long serviceId,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoService.uploadPercentagePhoto(file, serviceId));
    }

    @GetMapping("/admin/percentage-photos/empty")
    public ResponseEntity<byte[]> getEmptyPhoto(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return photoService.getPercentageEmptyPhoto();
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/admin/percentage-photos/upload-empty", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<PhotoResponse> uploadEmptyPhoto(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoService.uploadPercentageEmptyPhoto(file));
    }
}
