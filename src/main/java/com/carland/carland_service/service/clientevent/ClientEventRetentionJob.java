package com.carland.carland_service.service.clientevent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * tr: Hər gün Bakı 03:20. Ham stack və breadcrumb silinir, issue sətri qalır.
 * en: Daily at 03:20 Baku. Raw stack and breadcrumbs go away; the issue row stays.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientEventRetentionJob {

    private final ClientEventMaintenance maintenance;

    @Scheduled(cron = "0 20 3 * * *", zone = "Asia/Baku")
    public void purge() {
        try {
            int removed = maintenance.purgeReceivedBeforeNow();
            log.info("client-events retention removed={}", removed);
        } catch (RuntimeException ex) {
            log.warn("client-events retention failed: {}", ex.getClass().getSimpleName());
        }
    }
}
