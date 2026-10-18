package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * tr: ddl-auto=update NOT NULL düşürmez. Panel ziyaretlerinin hyper_record_id'si yoxdur, ona görə burada açılır.
 *     Təkrar işləsə də zərərsizdir (scripts/visit_panel_completion.sql ilə eyni).
 * en: ddl-auto=update never drops NOT NULL. Panel visits have no hyper_record_id, so it is relaxed here.
 *     Safe to run on every boot (same as scripts/visit_panel_completion.sql).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitPanelSchemaFix implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE visits ALTER COLUMN hyper_record_id DROP NOT NULL");
        } catch (Exception ex) {
            log.warn("visits.hyper_record_id DROP NOT NULL skipped: {}", ex.getMessage());
        }
    }
}
