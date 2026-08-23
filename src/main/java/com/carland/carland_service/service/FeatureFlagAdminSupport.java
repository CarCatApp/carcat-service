package com.carland.carland_service.service;

import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Admin katalog/audit sayfalama ve flag adı çözümlemesi.
 * en: Admin catalog/audit paging and flag-name resolution.
 */
public final class FeatureFlagAdminSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;

    private FeatureFlagAdminSupport() {
    }

    /**
     * tr: page &gt;= 0; size yalnızca 10 veya 25.
     * en: page &gt;= 0; size must be 10 or 25.
     */
    public static PageRequest pageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size != 10 && size != 25) {
            throw new IllegalArgumentException("size must be 10 or 25");
        }
        return PageRequest.of(page, size);
    }

    public static Map<String, Object> envelope(Page<?> page, List<Map<String, Object>> content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", content);
        body.put("page", page.getNumber());
        body.put("size", page.getSize());
        body.put("totalElements", page.getTotalElements());
        body.put("totalPages", page.getTotalPages());
        return body;
    }

    /**
     * tr: Soft-delete flag adı null döner (katalogda "in a flag" yerine boş).
     * en: Soft-deleted flags resolve to null (catalog shows empty instead of a stale name).
     */
    public static String liveFlagName(FeatureFlag flag) {
        if (flag == null || flag.getDeletedAt() != null) {
            return null;
        }
        return flag.getName();
    }

    public static Map<String, Object> endpointDto(FeatureFlagEndpoint ep) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id", ep.getId());
        e.put("method", ep.getHttpMethod());
        e.put("path", ep.getPathPattern());
        e.put("neverGuard", ep.isNeverGuard());
        String flagName = liveFlagName(ep.getFlag());
        boolean claimed = flagName != null;
        e.put("claimed", claimed);
        e.put("inFlag", claimed);
        e.put("flagName", flagName);
        return e;
    }

    public static Map<String, Object> auditDto(FeatureFlagAudit row) {
        Map<String, Object> dto = new LinkedHashMap<>();
        String action = row.getHttpMethod();
        String flagName = resolveStoredFlagName(row);
        String path = row.getPathPattern() != null ? row.getPathPattern() : "";
        String roleOrTarget = resolveAuditTarget(action, path, row);
        dto.put("at", row.getCreatedAt() != null ? row.getCreatedAt().toString() : null);
        dto.put("actor", row.getActor());
        dto.put("action", action);
        dto.put("flagName", flagName);
        dto.put("method", action);
        dto.put("path", path);
        dto.put("role", row.getRole() != null ? row.getRole().name() : null);
        dto.put("target", roleOrTarget);
        dto.put("oldState", row.getOldState() != null ? row.getOldState().name() : null);
        dto.put("newState", row.getNewState() != null ? row.getNewState().name() : null);
        dto.put("appVersion", row.getAppVersion());
        dto.put("change", formatChange(flagName, roleOrTarget, row));
        return dto;
    }

    /**
     * tr: Okumada stored flag_name öncelikli; yoksa legacy path'ten (STATE satırı) çıkarır.
     * en: Prefer stored flag_name; fall back to legacy path text (STATE rows).
     */
    public static String resolveStoredFlagName(FeatureFlagAudit row) {
        if (row == null) {
            return null;
        }
        if (StringUtils.hasText(row.getFlagName())) {
            return row.getFlagName().trim();
        }
        String path = row.getPathPattern();
        if ("STATE".equals(row.getHttpMethod()) && StringUtils.hasText(path)) {
            String[] parts = path.trim().split("\\s+");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                return parts[0];
            }
        }
        return null;
    }

    private static String resolveAuditTarget(String action, String path, FeatureFlagAudit row) {
        if ("STATE".equals(action)) {
            if (row.getRole() != null && row.getRole().name() != null
                    && !"ADMIN".equals(row.getRole().name())) {
                return row.getRole().name();
            }
            String[] parts = path.trim().split("\\s+");
            return parts.length > 1 ? parts[parts.length - 1] : path;
        }
        return path;
    }

    /**
     * tr: UI satırı: FLAG_NAME ROLE → NEW (ör. TEST_FLOW USER → HIDDEN).
     * en: UI row: FLAG_NAME ROLE → NEW (e.g. TEST_FLOW USER → HIDDEN).
     */
    public static String formatChange(String flagName, String target, FeatureFlagAudit row) {
        String left = StringUtils.hasText(flagName) ? flagName.trim() : "—";
        if (StringUtils.hasText(target) && !target.equals(left)) {
            boolean alreadyContains = target.contains(left);
            if (!alreadyContains) {
                left = left + " " + target.trim();
            } else {
                left = target.trim();
            }
        }
        String right = row.getNewState() != null ? row.getNewState().name() : "—";
        return left + " → " + right;
    }
}
