package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminServiceCategorySaveRequest;
import com.carland.carland_service.dto.response.AdminServiceCategoryRow;
import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.PhotoService;
import com.carland.carland_service.service.ServiceCategoryService;
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
 * tr: Admin panel hizmet kategorileri ve ikon yükleme. Cookie auth. Silme yok; pasifleştirme active=false.
 * en: Admin panel service categories and icon upload. Cookie auth. No delete; deactivate with active=false.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminServiceCategoryController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final ServiceCategoryService serviceCategoryService;
    private final PhotoService photoService;

    /**
     * tr: Kategori tablosu. Panel admin değilse login'e döner.
     * en: Category table. Redirects to login when the caller is not a panel admin.
     */
    @GetMapping(value = "/admin/service-categories", produces = MediaType.TEXT_HTML_VALUE)
    public String page(HttpServletRequest request, Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        model.addAttribute("categories", serviceCategoryService.adminRows());
        return "service-categories";
    }

    /**
     * tr: Kategori oluşturur veya günceller. code yalnızca yeni kayıtta yazılır.
     * en: Creates or updates a category. code is written only on create.
     */
    @PostMapping(value = "/admin/service-categories/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AdminServiceCategoryRow> save(@RequestBody AdminServiceCategorySaveRequest body,
                                                        HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(serviceCategoryService.save(body));
    }

    /**
     * tr: Kategori ikonunu byte olarak döner. Foto yoksa 404.
     * en: Returns the category icon bytes. 404 when the photo is missing.
     */
    @GetMapping("/admin/service-categories/icon")
    public ResponseEntity<byte[]> icon(@RequestParam Long categoryId, HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return photoService.getServiceCategoryPhoto(categoryId);
        } catch (ResourceNotFoundException | MissingFieldException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * tr: Kategori ikonunu yükler. PhotoService Redis'i commit sonrası siler.
     * en: Uploads the category icon. PhotoService evicts Redis after commit.
     */
    @PostMapping(value = "/admin/service-categories/icon/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<PhotoResponse> uploadIcon(@RequestPart("file") MultipartFile file,
                                                    @RequestParam Long categoryId,
                                                    HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoService.uploadServiceCategoryPhoto(file, categoryId));
    }

    /**
     * tr: Kategori ikonunu siler.
     * en: Deletes the category icon.
     */
    @PostMapping("/admin/service-categories/icon/delete")
    @ResponseBody
    public ResponseEntity<PhotoResponse> deleteIcon(@RequestParam Long categoryId, HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoService.deleteServiceCategoryPhoto(categoryId));
    }
}
