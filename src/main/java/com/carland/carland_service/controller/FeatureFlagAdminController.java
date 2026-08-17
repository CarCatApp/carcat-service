package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCurrentRequest;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.service.FeatureFlagService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * tr: Admin feature-flag sayfası ve grid/version/audit JSON API'leri (session login).
 * en: Admin feature-flag page plus grid/version/audit JSON APIs (session login).
 */
@Controller
@RequiredArgsConstructor
public class FeatureFlagAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final FeatureFlagService featureFlagService;

    @GetMapping("/admin/feature-flags")
    public String page(HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "feature-flags";
    }

    @GetMapping("/admin/api/feature-flags/grid")
    @ResponseBody
    public ResponseEntity<?> grid(
            @RequestParam(required = false) String version,
            HttpSession session
    ) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "login required"));
        }
        return ResponseEntity.ok(featureFlagService.grid(version));
    }

    @PostMapping("/admin/api/feature-flags/state")
    @ResponseBody
    public ResponseEntity<?> updateState(
            @RequestBody FeatureFlagStateUpdateRequest request,
            HttpSession session
    ) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "login required"));
        }
        String actor = (String) session.getAttribute("ADMIN_USERNAME");
        try {
            FeatureFlagAudit audit = featureFlagService.updateState(request, actor);
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("ok", true);
            body.put("actor", audit.getActor());
            body.put("oldState", audit.getOldState() != null ? audit.getOldState().name() : null);
            body.put("newState", audit.getNewState() != null ? audit.getNewState().name() : null);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    @PostMapping("/admin/api/feature-flags/versions")
    @ResponseBody
    public ResponseEntity<?> createVersion(
            @RequestBody FeatureFlagVersionCreateRequest request,
            HttpSession session
    ) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "login required"));
        }
        try {
            AppVersion created = featureFlagService.createVersion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "semver", created.getSemver(),
                    "current", created.isCurrent()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/api/feature-flags/versions/current")
    @ResponseBody
    public ResponseEntity<?> setCurrent(
            @RequestBody FeatureFlagVersionCurrentRequest request,
            HttpSession session
    ) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "login required"));
        }
        try {
            AppVersion updated = featureFlagService.setCurrentVersion(request.getSemver());
            return ResponseEntity.ok(Map.of(
                    "semver", updated.getSemver(),
                    "current", updated.isCurrent()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    @ResponseBody
    public ResponseEntity<?> audit(
            @RequestParam(required = false) String version,
            HttpSession session
    ) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "login required"));
        }
        List<FeatureFlagAudit> rows = featureFlagService.recentAudit(version);
        return ResponseEntity.ok(rows);
    }

    private boolean isLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("ADMIN_LOGIN"));
    }
}
