package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.AdminPushAudienceRequest;
import com.carland.carland_service.dto.request.AdminPushSendRequest;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.AdminPushCampaignService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * tr: Admin push JSON: filtre seçenekleri, kitle önizlemesi, kampanya kuyruğu ve durum.
 * en: Admin push JSON: filter options, audience preview, campaign queue and status.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class AdminPushRestController {

    private final AdminPushCampaignService adminPushCampaignService;
    private final AdminAccessService adminAccessService;

    @GetMapping(value = "/admin/push-notifications/filters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> filters(HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(adminPushCampaignService.filters());
    }

    @PostMapping(value = "/admin/push-notifications/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> preview(
            @RequestBody(required = false) AdminPushAudienceRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(adminPushCampaignService.preview(body == null ? new AdminPushAudienceRequest() : body));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/admin/push-notifications/campaigns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queue(
            @RequestBody AdminPushSendRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(adminPushCampaignService.queue(body, adminAccessService.actor(request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping(value = "/admin/push-notifications/campaigns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> recent(HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(adminPushCampaignService.recent());
    }

    @GetMapping(value = "/admin/push-notifications/campaigns/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> one(@PathVariable Long id, HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(adminPushCampaignService.get(id));
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
}
