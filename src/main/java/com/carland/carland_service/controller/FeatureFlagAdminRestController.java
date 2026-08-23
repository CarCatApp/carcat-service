package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.FeatureFlagAttachRequest;
import com.carland.carland_service.dto.request.FeatureFlagEndpointWriteRequest;
import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCurrentRequest;
import com.carland.carland_service.dto.request.FeatureFlagWriteRequest;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.security.AdminAccessService;
import com.carland.carland_service.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * tr: PO sözleşmeli feature-flag REST (id path, Bearer veya cookie). Panel HTML ayrı kalır.
 * en: PO-shaped feature-flag REST (id in path, Bearer or cookie). HTML page stays separate.
 */
@Tag(name = "admin-feature-flags", description = "Panel ADMIN JWT only. Other JWTs → 403 Admin not found.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class FeatureFlagAdminRestController {

    private final FeatureFlagService featureFlagService;
    private final AdminAccessService adminAccessService;

    @Operation(summary = "List flags", description = "Live flags (soft-deleted hidden). Accept: application/json required.")
    @GetMapping(value = "/admin/feature-flags", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(featureFlagService.listFlags());
    }

    @Operation(summary = "Get flag by id")
    @GetMapping(value = "/admin/feature-flags/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getOne(
            @PathVariable Long id,
            @RequestParam(required = false) String version,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(featureFlagService.flagDetail(id, version));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Create flag")
    @PostMapping(value = "/admin/feature-flags", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(
            @RequestBody FeatureFlagWriteRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            FeatureFlag flag = featureFlagService.createFlag(body, adminAccessService.actor(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(featureFlagService.flagDetail(flag.getId(), null));
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Update flag (name immutable)")
    @PutMapping(value = "/admin/feature-flags/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody FeatureFlagWriteRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.updateFlag(id, body, adminAccessService.actor(request));
            return ResponseEntity.ok(featureFlagService.flagDetail(id, null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Soft-delete flag")
    @DeleteMapping("/admin/feature-flags/{id:\\d+}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.deleteFlag(id, adminAccessService.actor(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Attach APIs to flag")
    @PostMapping(value = "/admin/feature-flags/{id:\\d+}/endpoints", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attach(
            @PathVariable Long id,
            @RequestBody FeatureFlagAttachRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
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

    @Operation(summary = "Detach API from flag")
    @DeleteMapping("/admin/feature-flags/{id:\\d+}/endpoints/{endpointId:\\d+}")
    public ResponseEntity<?> detach(
            @PathVariable Long id,
            @PathVariable Long endpointId,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.detachEndpoint(id, endpointId, adminAccessService.actor(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "List unclaimed catalog APIs")
    @GetMapping(value = "/admin/available-endpoints", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> available(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(featureFlagService.availableEndpoints(page, size));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "List all catalog APIs (paginated)")
    @GetMapping(value = "/admin/endpoints", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listEndpoints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(featureFlagService.listEndpoints(page, size));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Update catalog API")
    @PutMapping(value = "/admin/endpoints/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateEndpoint(
            @PathVariable Long id,
            @RequestBody FeatureFlagEndpointWriteRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(featureFlagService.updateEndpoint(id, body, adminAccessService.actor(request)));
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Delete catalog API")
    @DeleteMapping("/admin/endpoints/{id:\\d+}")
    public ResponseEntity<?> deleteEndpoint(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            featureFlagService.deleteEndpoint(id, adminAccessService.actor(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return conflict(ex);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Set role state (PATCH)")
    @PatchMapping(value = "/admin/feature-flags/{id:\\d+}/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patchState(
            @PathVariable Long id,
            @RequestBody FeatureFlagStateUpdateRequest body,
            HttpServletRequest request
    ) {
        return writeState(id, body, request);
    }

    @Operation(summary = "Set role state (POST if nginx blocks PATCH)")
    @PostMapping(value = "/admin/feature-flags/{id:\\d+}/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postState(
            @PathVariable Long id,
            @RequestBody FeatureFlagStateUpdateRequest body,
            HttpServletRequest request
    ) {
        return writeState(id, body, request);
    }

    @Operation(summary = "Flag audit (includes soft-deleted)")
    @GetMapping(value = "/admin/feature-flags/{id:\\d+}/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> audit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(featureFlagService.auditForFlag(id, page, size));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Global change history (all flags)")
    @GetMapping(value = "/admin/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String flagName,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        try {
            return ResponseEntity.ok(featureFlagService.listAudit(page, size, flagName));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Admin snapshot (panel one-shot)")
    @GetMapping(value = "/admin/feature-flags/snapshot", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> snapshot(
            @RequestParam(required = false) String version,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(featureFlagService.adminSnapshot(version));
    }

    @Operation(summary = "List app versions")
    @GetMapping(value = "/admin/feature-flags/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> versions(HttpServletRequest request) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(featureFlagService.listVersions());
    }

    @Operation(summary = "Create app version")
    @PostMapping(value = "/admin/feature-flags/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createVersion(
            @RequestBody FeatureFlagVersionCreateRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
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

    @Operation(summary = "Set current app version")
    @PostMapping(value = "/admin/feature-flags/versions/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setCurrent(
            @RequestBody FeatureFlagVersionCurrentRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
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

    private ResponseEntity<?> writeState(
            Long id,
            FeatureFlagStateUpdateRequest body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = guard(request);
        if (denied != null) {
            return denied;
        }
        body.setFlagId(id);
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

    private ResponseEntity<?> guard(HttpServletRequest request) {
        return switch (adminAccessService.inspect(request)) {
            case MISSING, INVALID -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "JWT token required"));
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin not found"));
            case OK -> null;
        };
    }

    private ResponseEntity<?> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
