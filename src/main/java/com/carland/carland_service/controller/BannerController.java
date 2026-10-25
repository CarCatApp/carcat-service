package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.service.AdminBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tr: Mobil banner listesi. Kimlik doğrulamaz. Boş sonuç {@code 200 []}.
 *     Yalnız showing=true ve status=active.
 * en: Mobile banner list. No authentication. Empty result is {@code 200 []}.
 *     Only showing=true and status=active.
 */
@Tag(name = "banners", description = "Public banner list. No JWT. Empty list is 200 [].")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    static final String JSON_UTF8 = "application/json;charset=UTF-8";

    private final AdminBannerService adminBannerService;

    @Operation(summary = "Visible banners",
            description = "Public. No headers required. Items with showing=true and status=active, newest id first. "
                    + "Never 401. Empty list is 200 [].")
    @GetMapping(produces = JSON_UTF8)
    public ResponseEntity<List<BannerItem>> list() {
        List<BannerItem> items = adminBannerService.visible();
        if (items == null) {
            items = List.of();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(JSON_UTF8))
                .cacheControl(CacheControl.noStore())
                .body(items);
    }
}
