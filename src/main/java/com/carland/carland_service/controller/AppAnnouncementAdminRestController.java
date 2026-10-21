package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AppAnnouncementActiveUpdateRequest;
import com.carland.carland_service.dto.request.AppAnnouncementWriteRequest;
import com.carland.carland_service.enums.AppAnnouncementPlatform;
import com.carland.carland_service.enums.AppAnnouncementType;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AppAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * tr: Panel telefon JWT ile duyuru yönetimi. Silme yok; yayından alma {@code active=false}.
 * en: Announcement admin for the panel-phone JWT. No delete; unpublish is {@code active=false}.
 */
@Tag(name = "admin-app-announcements", description = "Panel phone JWT only. Other JWTs → 403 Admin not found.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class AppAnnouncementAdminRestController {

    private final AppAnnouncementService appAnnouncementService;
    private final AdminAccessService adminAccessService;

    @Operation(summary = "List announcements")
    @GetMapping(value = "/admin/app-announcements", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(
            @RequestParam(required = false) String active,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String platform,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(appAnnouncementService.list(
                    parseActive(active),
                    parseType(type),
                    parsePlatform(platform)
            ));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @Operation(summary = "Preview the public selection")
    @GetMapping(value = "/admin/app-announcements/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> preview(
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String at,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        Instant instant;
        try {
            instant = parseAt(at);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
        return ResponseEntity.ok(appAnnouncementService.preview(version, platform, lang, instant));
    }

    @Operation(summary = "Get one announcement with translations")
    @GetMapping(value = "/admin/app-announcements/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable long id, HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(appAnnouncementService.get(id));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @Operation(summary = "Create a draft announcement")
    @PostMapping(value = "/admin/app-announcements", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody AppAnnouncementWriteRequest body, HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(appAnnouncementService.create(body, adminAccessService.actor(request)));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @Operation(summary = "Update an announcement")
    @PutMapping(value = "/admin/app-announcements/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
            @PathVariable long id,
            @RequestBody AppAnnouncementWriteRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(appAnnouncementService.update(id, body, adminAccessService.actor(request)));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @Operation(summary = "Publish or unpublish")
    @PostMapping(value = "/admin/app-announcements/{id:\\d+}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setActive(
            @PathVariable long id,
            @RequestBody AppAnnouncementActiveUpdateRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        if (body == null || body.getActive() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "active is required"));
        }
        try {
            return ResponseEntity.ok(appAnnouncementService.setActive(id, body.getActive(), adminAccessService.actor(request)));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @Operation(summary = "Copy as a new inactive announcement")
    @PostMapping(value = "/admin/app-announcements/{id:\\d+}/copy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> copy(@PathVariable long id, HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(appAnnouncementService.copy(id, adminAccessService.actor(request)));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    private ResponseEntity<?> guard(HttpServletRequest request) {
        return switch (adminAccessService.inspect(request)) {
            case MISSING, INVALID -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "JWT token required"));
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin not found"));
            case OK -> null;
        };
    }

    private static ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Invalid announcement" : ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private static Boolean parseActive(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.equalsIgnoreCase("true")) {
            return true;
        }
        if (raw.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("active must be true or false");
    }

    private static AppAnnouncementType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AppAnnouncementType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("type must be UPDATE, INFO, or MAINTENANCE");
        }
    }

    private static AppAnnouncementPlatform parsePlatform(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AppAnnouncementPlatform.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("platform must be ALL, IOS, or ANDROID");
        }
    }

    private static Instant parseAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("at must be ISO-8601 UTC");
        }
    }
}
