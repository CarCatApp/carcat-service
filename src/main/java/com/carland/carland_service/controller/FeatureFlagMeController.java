package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.FeatureFlagMeItem;
import com.carland.carland_service.service.FeatureFlagService;
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
@RestController
@RequestMapping("/api/v1/feature-flags")
@RequiredArgsConstructor
public class FeatureFlagMeController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader(value = "role", required = false) String role,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion
    ) {
        try {
            return ResponseEntity.ok(featureFlagService.me(role, appVersion));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }
}
