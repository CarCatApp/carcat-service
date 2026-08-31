package com.carland.carland_service.service;

import com.carland.carland_service.util.SemVer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tr: Eski path-merkezli role_state temizliği; per-flag minAvailableVersion göçü;
 *     ardından global app_version kataloğu ve version_id düşülür.
 * en: Legacy path-centric cleanup; per-flag minAvailableVersion migration;
 *     then drop the global app_version catalog and version_id.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class FeatureFlagLegacyCleanup implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE feature_flag_role_state DROP CONSTRAINT IF EXISTS uk_ffrs_endpoint_version_role");
        } catch (Exception ex) {
            log.debug("drop legacy unique: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE feature_flag_role_state ALTER COLUMN endpoint_id DROP NOT NULL");
        } catch (Exception ex) {
            log.debug("endpoint_id nullability: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("DELETE FROM feature_flag_role_state WHERE flag_id IS NULL");
        } catch (Exception ex) {
            log.debug("role_state cleanup: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE feature_flag_role_state DROP COLUMN IF EXISTS endpoint_id");
        } catch (Exception ex) {
            log.debug("drop endpoint_id: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("UPDATE feature_flag_endpoint SET flag_id = NULL WHERE flag_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM feature_flag f WHERE f.id = feature_flag_endpoint.flag_id AND f.deleted_at IS NULL)");
        } catch (Exception ex) {
            log.debug("endpoint flag_id cleanup: {}", ex.getMessage());
        }
        migratePerFlagMinVersion();
        dropVersionCatalog();
    }

    private void migratePerFlagMinVersion() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE feature_flag ADD COLUMN IF NOT EXISTS min_available_version varchar(32)");
        } catch (Exception ex) {
            log.debug("add min_available_version: {}", ex.getMessage());
        }
        if (!columnExists("feature_flag_role_state", "version_id") || !tableExists("app_version")) {
            fillNullMins();
            return;
        }
        try {
            Map<Long, String> minByFlag = new HashMap<>();
            jdbcTemplate.query(
                    """
                            SELECT s.flag_id, v.semver
                            FROM feature_flag_role_state s
                            JOIN app_version v ON v.id = s.version_id
                            WHERE s.flag_id IS NOT NULL AND v.semver IS NOT NULL
                            """,
                    rs -> {
                        long flagId = rs.getLong("flag_id");
                        String semver = rs.getString("semver");
                        minByFlag.merge(flagId, semver, FeatureFlagLegacyCleanup::lowerSemVer);
                    });
            for (Map.Entry<Long, String> e : minByFlag.entrySet()) {
                jdbcTemplate.update(
                        "UPDATE feature_flag SET min_available_version = ? WHERE id = ?",
                        e.getValue(), e.getKey());
            }
            collapseRoleStatesToCurrentGrid();
            fillNullMins();
            log.info("Feature-flag per-flag minAvailableVersion migration done, flags={}", minByFlag.size());
        } catch (Exception ex) {
            log.warn("per-flag version migration: {}", ex.getMessage());
        }
    }

    private void collapseRoleStatesToCurrentGrid() {
        List<StateRow> rows = jdbcTemplate.query(
                """
                        SELECT s.id, s.flag_id, s.version_id, v.semver, COALESCE(v.is_current, false) AS is_current
                        FROM feature_flag_role_state s
                        LEFT JOIN app_version v ON v.id = s.version_id
                        WHERE s.flag_id IS NOT NULL
                        """,
                (rs, n) -> new StateRow(
                        rs.getLong("id"),
                        rs.getLong("flag_id"),
                        rs.getObject("version_id") != null ? rs.getLong("version_id") : null,
                        rs.getString("semver"),
                        rs.getBoolean("is_current")));
        Map<Long, List<StateRow>> byFlag = new HashMap<>();
        for (StateRow row : rows) {
            byFlag.computeIfAbsent(row.flagId, k -> new ArrayList<>()).add(row);
        }
        Set<Long> keep = new HashSet<>();
        for (List<StateRow> group : byFlag.values()) {
            List<StateRow> current = group.stream().filter(r -> r.current).toList();
            List<StateRow> chosen = current.isEmpty() ? rowsWithHighestSemVer(group) : current;
            for (StateRow row : chosen) {
                keep.add(row.id);
            }
        }
        for (StateRow row : rows) {
            if (!keep.contains(row.id)) {
                jdbcTemplate.update("DELETE FROM feature_flag_role_state WHERE id = ?", row.id);
            }
        }
    }

    private static List<StateRow> rowsWithHighestSemVer(List<StateRow> group) {
        String best = null;
        for (StateRow row : group) {
            if (row.semver == null) {
                continue;
            }
            if (best == null || SemVer.isAtLeast(row.semver, best)) {
                best = row.semver;
            }
        }
        if (best == null) {
            return group;
        }
        String keep = best;
        return group.stream().filter(r -> keep.equals(r.semver)).toList();
    }

    private void fillNullMins() {
        try {
            jdbcTemplate.update(
                    "UPDATE feature_flag SET min_available_version = '0.0.0' WHERE min_available_version IS NULL OR btrim(min_available_version) = ''");
        } catch (Exception ex) {
            log.debug("fill null mins: {}", ex.getMessage());
        }
    }

    /**
     * tr: Per-flag min göçünden sonra global katalog ve version_id FK kalkar.
     * en: After per-flag min migration, drop the global catalog and version_id FK.
     */
    private void dropVersionCatalog() {
        boolean hadCatalog = columnExists("feature_flag_role_state", "version_id") || tableExists("app_version");
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE feature_flag_role_state DROP CONSTRAINT IF EXISTS uk_ffrs_flag_version_role");
        } catch (Exception ex) {
            log.debug("drop uk_ffrs_flag_version_role: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE feature_flag_role_state DROP COLUMN IF EXISTS version_id CASCADE");
        } catch (Exception ex) {
            log.debug("drop version_id: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS app_version CASCADE");
        } catch (Exception ex) {
            log.debug("drop app_version: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ffrs_flag_role') THEN
                        ALTER TABLE feature_flag_role_state
                          ADD CONSTRAINT uk_ffrs_flag_role UNIQUE (flag_id, role);
                      END IF;
                    END $$;
                    """);
        } catch (Exception ex) {
            log.debug("uk_ffrs_flag_role: {}", ex.getMessage());
        }
        if (hadCatalog) {
            log.info("Feature-flag app_version catalog dropped");
        }
    }

    private static String lowerSemVer(String a, String b) {
        if (a == null || a.isBlank()) {
            return b;
        }
        if (b == null || b.isBlank()) {
            return a;
        }
        return SemVer.isAtLeast(a, b) ? b : a;
    }

    private boolean tableExists(String table) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, table);
        return n != null && n > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer n = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                        """,
                Integer.class, table, column);
        return n != null && n > 0;
    }

    private record StateRow(long id, long flagId, Long versionId, String semver, boolean current) {
    }
}
