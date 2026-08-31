package com.carland.carland_service.controller;

import com.carland.carland_service.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * tr: Mobil feature-flag okuma; Kong/JWT role header'ına göre yalnızca çağıran rolün map'ini döner.
 * en: Mobile feature-flag read; returns only the caller role's map from the Kong/JWT role header.
 */
@Tag(name = "feature-flags-me", description = "Mobile feature-flag read. Caller JWT role (USER/ADMIN/SUPER_ADMIN/BOSS). Not panel-admin-only.")
@RestController
@RequestMapping("/api/v1/feature-flags")
@RequiredArgsConstructor
public class FeatureFlagMeController {

    private final FeatureFlagService featureFlagService;

    @Operation(summary = "GET /me (caller role)",
            description = "Returns named flags for the Kong/JWT `role` header and `X-App-Version`. "
                    + "A flag is omitted when the client version is below minAvailableVersion. "
                    + "Any authenticated app role. Panel phone is not required.")
    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader(value = "role", required = false) String role,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion
    ) {
        if (role == null || role.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "role header required"));
        }
        try {
            return ResponseEntity.ok(featureFlagService.me(role, appVersion));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        }
    }
}
