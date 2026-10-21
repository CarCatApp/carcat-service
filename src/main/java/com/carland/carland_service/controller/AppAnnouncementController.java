package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.AppAnnouncementActiveItem;
import com.carland.carland_service.service.AppAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * tr: Mobil cold start duyurusu. Kimlik doğrulamaz; Authorization yok sayılır. Boş sonuç {@code 200 []}.
 * en: Mobile cold-start announcements. No authentication; Authorization is ignored. Empty result is {@code 200 []}.
 */
@Slf4j
@Tag(name = "app-announcements", description = "Public cold-start announcements. No JWT. Empty list is 200 [].")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/app-announcements")
@RequiredArgsConstructor
public class AppAnnouncementController {

    static final String JSON_UTF8 = "application/json;charset=UTF-8";

    private final AppAnnouncementService appAnnouncementService;

    @Operation(summary = "Active announcements",
            description = "Public. Headers are optional. Invalid headers narrow the list; they do not fail the call. "
                    + "At most 5 items. Never 401.")
    @GetMapping(value = "/active", produces = JSON_UTF8)
    public ResponseEntity<?> active(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
            @RequestHeader(value = "X-Platform", required = false) String platform
    ) {
        try {
            List<AppAnnouncementActiveItem> items = appAnnouncementService.active(acceptLanguage, appVersion, platform);
            if (items == null) {
                items = List.of();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(JSON_UTF8))
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .varyBy("Accept-Language", "X-App-Version", "X-Platform")
                    .body(items);
        } catch (Exception ex) {
            log.warn("app-announcements active failed: {}", ex.toString());
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.parseMediaType(JSON_UTF8))
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("message", "Unable to load announcements"));
        }
    }
}
