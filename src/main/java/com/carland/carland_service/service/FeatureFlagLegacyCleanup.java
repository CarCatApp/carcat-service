package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * tr: Eski path-merkezli role_state satırlarını temizler; endpoint_id NOT NULL ise kaldırır.
 * en: Clears legacy path-centric role_state rows; drops endpoint_id NOT NULL if present.
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
    }
}
