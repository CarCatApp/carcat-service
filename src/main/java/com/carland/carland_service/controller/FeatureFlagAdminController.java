package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.FeatureFlagAttachRequest;
import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCurrentRequest;
import com.carland.carland_service.dto.request.FeatureFlagWriteRequest;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * tr: Admin feature-flag sayfası ve named-flag CRUD/attach/state JSON API'leri (JWT cookie).
 * en: Admin feature-flag page plus named-flag CRUD/attach/state JSON APIs (JWT cookie).
 */
@Controller
@RequiredArgsConstructor
public class FeatureFlagAdminController {

    private static final String ADMIN_URL = "https://digital-innovation.agency";

    private final FeatureFlagService featureFlagService;
    private final AdminAccessService adminAccessService;

    @GetMapping("/admin/feature-flags")
    public String page(HttpServletRequest request) {
        if (!adminAccessService.isPanelAdmin(request)) {
            return "redirect:" + ADMIN_URL + "/admin/";
        }
        return "feature-flags";
    }

    @GetMapping("/admin/api/feature-flags/snapshot")
    @ResponseBody
    public ResponseEntity<?> snapshot(
            @RequestParam(required = false) String version,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(featureFlagService.adminSnapshot(version));
    }

    @PostMapping("/admin/api/feature-flags")
    @ResponseBody
    public ResponseEntity<?> createFlag(
            @RequestBody FeatureFlagWriteRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            FeatureFlag flag = featureFlagService.createFlag(body, adminAccessService.actor(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(flagDto(flag));
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/admin/api/feature-flags/{id}")
    @ResponseBody
    public ResponseEntity<?> updateFlag(
            @PathVariable Long id,
            @RequestBody FeatureFlagWriteRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            FeatureFlag flag = featureFlagService.updateFlag(id, body, adminAccessService.actor(request));
            return ResponseEntity.ok(flagDto(flag));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/admin/api/feature-flags/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteFlag(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.deleteFlag(id, adminAccessService.actor(request));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/api/feature-flags/{id}/endpoints")
    @ResponseBody
    public ResponseEntity<?> attach(
            @PathVariable Long id,
            @RequestBody FeatureFlagAttachRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.attachEndpoints(id, body, adminAccessService.actor(request));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/admin/api/feature-flags/{id}/endpoints/{endpointId}")
    @ResponseBody
    public ResponseEntity<?> detach(
            @PathVariable Long id,
            @PathVariable Long endpointId,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.detachEndpoint(id, endpointId, adminAccessService.actor(request));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/api/feature-flags/state")
    @ResponseBody
    public ResponseEntity<?> updateState(
            @RequestBody FeatureFlagStateUpdateRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            FeatureFlagAudit audit = featureFlagService.updateState(body, adminAccessService.actor(request));
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", true);
            res.put("actor", audit.getActor());
            res.put("oldState", audit.getOldState() != null ? audit.getOldState().name() : null);
            res.put("newState", audit.getNewState() != null ? audit.getNewState().name() : null);
            return ResponseEntity.ok(res);
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
            @RequestBody FeatureFlagVersionCreateRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            AppVersion created = featureFlagService.createVersion(body);
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
            @RequestBody FeatureFlagVersionCurrentRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        try {
            AppVersion updated = featureFlagService.setCurrentVersion(body.getSemver());
            return ResponseEntity.ok(Map.of(
                    "semver", updated.getSemver(),
                    "current", updated.isCurrent()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private ResponseEntity<?> requireAdmin(HttpServletRequest request) {
        if (adminAccessService.isPanelAdmin(request)) {
            return null;
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Admin not found"));
    }

    private ResponseEntity<?> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> flagDto(FeatureFlag flag) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", flag.getId());
        dto.put("name", flag.getName());
        dto.put("description", flag.getDescription());
        dto.put("defaultState", flag.getDefaultState() != null ? flag.getDefaultState().name() : null);
        return dto;
    }
}
