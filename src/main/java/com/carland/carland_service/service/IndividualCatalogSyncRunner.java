package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * tr: Boot'ta çalışmaz. Bean değildir. individual_service_sources bir kez doldu;
 *     her deploy'da silip yeniden INSERT edilmez.
 * en: Does not run on boot. Not a bean. individual_service_sources was filled once;
 *     it is not deleted and inserted again on every deploy.
 */
@Slf4j
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
