package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * tr: Açılışta fərdi kataloğu services tablosundan doldurur. Mock seeder çalışmaz.
 * en: Fills the individual catalog from the services table on startup. The mock seeder does not run.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class IndividualCatalogSyncRunner implements ApplicationRunner {

    private final IndividualCatalogSync sync;

    @Override
    public void run(ApplicationArguments args) {
        try {
            sync.sync();
        } catch (Exception ex) {
            log.warn("INDIVIDUAL_CATALOG_SYNC_FAIL {}", ex.toString());
        }
    }
}
