package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminOfferedServiceSaveRequest;
import com.carland.carland_service.dto.response.AdminOfferedServiceRow;
import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.OfferedCatalogService;
import com.carland.carland_service.service.PhotoService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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
    private final PhotoService photoService;

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
        model.addAttribute("filters", offeredCatalogService.adminFilters());
        return "offered-services";
    }

    /**
     * tr: Panel önizlemesi. Foto yoksa 404.
     * en: Panel preview. 404 when the icon is missing.
     */
    @GetMapping("/admin/offered-services/photo")
    public ResponseEntity<byte[]> photo(@RequestParam("offeredServiceId") Long offeredServiceId,
                                        HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return photoService.getOfferedServicePhoto(offeredServiceId);
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * tr: Panelden ikon yükler. offeredServiceId kayıtlı satırın id'sidir.
     * en: Uploads an icon from the panel. offeredServiceId is the saved row id.
     */
    @PostMapping(value = "/admin/offered-services/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<PhotoResponse> uploadPhoto(@RequestPart("file") MultipartFile file,
                                                     @RequestParam("offeredServiceId") Long offeredServiceId,
                                                     HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoService.uploadOfferedServicePhoto(file, offeredServiceId));
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
