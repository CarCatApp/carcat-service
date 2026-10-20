package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminColorDeleteRequest;
import com.carland.carland_service.dto.request.AdminColorNameRow;
import com.carland.carland_service.dto.request.AdminColorNameSaveRequest;
import com.carland.carland_service.dto.request.AdminColorSortMoveRequest;
import com.carland.carland_service.dto.response.AdminColorSortRow;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.ColorCatalogOrder;
import com.carland.carland_service.service.ColorSortService;
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

import java.util.List;

/**
 * tr: Admin color sort sayfası. Sıra dile göredir.
 * en: Admin color sort page. Order is per language.
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class AdminColorSortController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final AdminAccessService adminAccessService;
    private final ColorSortService colorSortService;

    @GetMapping(value = "/admin/color-sort", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@RequestParam(required = false) String lang,
                       HttpServletRequest request,
                       Model model) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        String key = ColorCatalogOrder.langKey(lang);
        model.addAttribute("lang", key);
        model.addAttribute("rows", colorSortService.rows(key));
        return "color-sort";
    }

    @PostMapping(value = "/admin/color-sort/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<AdminColorSortRow>> move(@RequestBody AdminColorSortMoveRequest body,
                                                        HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null) {
            body = new AdminColorSortMoveRequest();
        }
        return ResponseEntity.ok(colorSortService.move(body.getLang(), body.getColorId(), body.getPosition()));
    }

    /**
     * tr: Değişen adları ve yeni renk satırlarını yazar. Hex ve sıra bu istekte değişmez.
     * en: Writes changed names and new color rows. Hex and order do not change on this request.
     */
    @PostMapping(value = "/admin/color-sort/names", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> saveNames(@RequestBody AdminColorNameSaveRequest body,
                                          HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<AdminColorNameRow> rows = body == null || body.getRows() == null ? List.of() : body.getRows();
        colorSortService.saveNames(rows);
        return ResponseEntity.ok().build();
    }

    /**
     * tr: Renk satırını siler. active kolonu olmadığı için kayıt DB'den gider.
     * en: Deletes a color row. There is no active column, so the row is removed from the DB.
     */
    @PostMapping(value = "/admin/color-sort/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> delete(@RequestBody AdminColorDeleteRequest body,
                                       HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        colorSortService.delete(body == null ? null : body.getColorId());
        return ResponseEntity.ok().build();
    }
}
