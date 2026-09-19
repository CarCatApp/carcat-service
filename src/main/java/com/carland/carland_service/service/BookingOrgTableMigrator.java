package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * tr: booking_partners / booking_branches → partners + branches. Eski tabloları siler.
 * en: Moves booking_partners / booking_branches into partners + branches, then drops the old tables.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BookingOrgTableMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrate();
        } catch (Exception ex) {
            log.warn("BOOKING_ORG_MIGRATE_FAIL {}", ex.toString());
        }
    }

    private void migrate() {
        jdbc.execute("ALTER TABLE partners ADD COLUMN IF NOT EXISTS contact_phone varchar(32)");
        jdbc.execute("ALTER TABLE partners ADD COLUMN IF NOT EXISTS contact_email varchar(128)");
        jdbc.execute("ALTER TABLE partners ADD COLUMN IF NOT EXISTS hq_user_id int8");
        jdbc.execute("ALTER TABLE partners ADD COLUMN IF NOT EXISTS created_at timestamp");
        jdbc.execute("ALTER TABLE partners ADD COLUMN IF NOT EXISTS updated_at timestamp");
        clearOrphanBranchIds();

        if (!tableExists("booking_partners") && !tableExists("booking_branches")) {
            return;
        }
        log.info("BOOKING_ORG_MIGRATE start");
        dropForeignKeys("booking_staff");
        dropForeignKeys("booking_branches");
        dropForeignKeys("branches");

        if (tableExists("booking_partners")) {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM booking_partners");
            for (Map<String, Object> row : rows) {
                long oldId = ((Number) row.get("id")).longValue();
                String name = stringVal(row.get("name"));
                Long existing = findPartnerIdByName(name);
                long newId;
                if (existing != null) {
                    newId = existing;
                    jdbc.update("""
                                    UPDATE partners SET
                                      contact_phone = COALESCE(contact_phone, ?),
                                      contact_email = COALESCE(contact_email, ?),
                                      hq_user_id = COALESCE(hq_user_id, ?),
                                      logo_url = COALESCE(logo_url, ?),
                                      updated_at = COALESCE(updated_at, ?)
                                    WHERE id = ?
                                    """,
                            stringVal(row.get("contact_phone")),
                            stringVal(row.get("contact_email")),
                            row.get("hq_user_id"),
                            stringVal(firstPresent(row, "photo", "logo_url")),
                            row.get("updated_at"),
                            newId);
                } else {
                    jdbc.update("""
                                    INSERT INTO partners (name, active, source, logo_url, contact_phone, contact_email,
                                                          hq_user_id, created_at, updated_at)
                                    VALUES (?, COALESCE(?, true), 'carcat', ?, ?, ?, ?, ?, ?)
                                    """,
                            name,
                            row.get("active"),
                            stringVal(firstPresent(row, "photo", "logo_url")),
                            stringVal(row.get("contact_phone")),
                            stringVal(row.get("contact_email")),
                            row.get("hq_user_id"),
                            row.get("created_at"),
                            row.get("updated_at"));
                    Long inserted = findPartnerIdByName(name);
                    if (inserted == null) {
                        continue;
                    }
                    newId = inserted;
                }
                if (newId != oldId) {
                    remapPartnerId("booking_staff", oldId, newId);
                    remapPartnerId("booking_staff_audit", oldId, newId);
                    remapPartnerId("booking_branches", oldId, newId);
                    remapPartnerId("branches", oldId, newId);
                }
            }
        }

        if (tableExists("booking_branches")) {
            if (tableExists("branches") && tableEmpty("branches")) {
                jdbc.execute("DROP TABLE branches");
            }
            if (!tableExists("branches")) {
                jdbc.execute("ALTER TABLE booking_branches RENAME TO branches");
            } else {
                jdbc.execute("DROP TABLE IF EXISTS booking_branches CASCADE");
            }
            if (tableExists("branches") && columnExists("branches", "photos")) {
                jdbc.execute("ALTER TABLE branches DROP COLUMN IF EXISTS photos");
            }
            try {
                jdbc.execute("SELECT setval(pg_get_serial_sequence('branches','id'), COALESCE((SELECT MAX(id) FROM branches), 1))");
            } catch (Exception ignored) {
                // sequence may not exist yet
            }
        }

        jdbc.execute("DROP TABLE IF EXISTS booking_partners CASCADE");
        clearOrphanBranchIds();
        log.info("BOOKING_ORG_MIGRATE done");
    }

    private void clearOrphanBranchIds() {
        if (!tableExists("booking_staff") || !tableExists("branches")
                || !columnExists("booking_staff", "branch_id")) {
            return;
        }
        int n = jdbc.update("""
                UPDATE booking_staff SET branch_id = NULL
                WHERE branch_id IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM branches b WHERE b.id = booking_staff.branch_id)
                """);
        if (n > 0) {
            log.warn("BOOKING_ORG_ORPHAN_BRANCH_CLEARED count={}", n);
        }
    }

    private void remapPartnerId(String table, long oldId, long newId) {
        if (!tableExists(table) || !columnExists(table, "partner_id")) {
            return;
        }
        jdbc.update("UPDATE " + table + " SET partner_id = ? WHERE partner_id = ?", newId, oldId);
    }

    private static String stringVal(Object value) {
        return value == null ? null : value.toString();
    }

    private static Object firstPresent(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    private Long findPartnerIdByName(String name) {
        return jdbc.query(
                "SELECT id FROM partners WHERE lower(name) = lower(?) ORDER BY id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                name);
    }

    private void dropForeignKeys(String table) {
        if (!tableExists(table)) {
            return;
        }
        jdbc.queryForList(
                """
                        SELECT con.conname FROM pg_constraint con
                        JOIN pg_class rel ON rel.oid = con.conrelid
                        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                        WHERE nsp.nspname = 'public' AND rel.relname = ? AND con.contype = 'f'
                        """,
                String.class, table
        ).forEach(con -> {
            try {
                jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + con);
            } catch (Exception ex) {
                log.debug("drop fk {}: {}", con, ex.getMessage());
            }
        });
    }

    private boolean columnExists(String table, String column) {
        Integer n = jdbc.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                        """,
                Integer.class, table, column);
        return n != null && n > 0;
    }

    private boolean tableEmpty(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null || n == 0;
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, table);
        return n != null && n > 0;
    }
}
