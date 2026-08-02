package com.carland.carland_service.service;

import com.carland.carland_service.service.VisitHistoryService;
import com.carland.carland_service.service.CarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs the percentage calculation + Hyper partner sync AFTER a car is added.
 *
 * <p>This is intentionally decoupled from addCar: it runs asynchronously and is fully
 * best-effort. Hyper being down or slow must never affect the addCar response; in that
 * case the percentages simply stay CREATED.</p>
 *
 * tr: Araç eklendikten SONRA yüzde hesaplamasını ve Hyper partner senkronizasyonunu asenkron çalıştırır.
 *     addCar akışından bilinçli olarak ayrıştırılmıştır; tamamen best-effort'tur ve Hyper'ın yavaş/erişilemez
 *     olması addCar cevabını asla etkilemez (bu durumda yüzdeler CREATED olarak kalır).
 * en: Runs the percentage calculation and Hyper partner sync asynchronously AFTER a car is added.
 *     Intentionally decoupled from the addCar flow; fully best-effort, so a slow/unavailable Hyper
 *     never affects the addCar response (percentages simply stay CREATED in that case).
 */
@Service
@Slf4j
public class AfterAddCarSyncService {

    private final CarService carService;
    private final VisitHistoryService visitHistoryService;

    public AfterAddCarSyncService(@Lazy CarService carService,
                                  VisitHistoryService visitHistoryService) {
        this.carService = carService;
        this.visitHistoryService = visitHistoryService;
    }

    /**
     * tr: Araç ekleme sonrası asenkron çalışır: önce servis yüzdelerini hesaplayıp kaydeder,
     *     ardından Hyper'dan servis geçmişini çekip partner verisini uygular. Her iki adım da
     *     hata izolasyonludur; istisnalar yutulur ve sadece loglanır.
     * en: Runs asynchronously after a car is added: first calculates and persists the service
     *     percentages, then pulls the Hyper service history and applies partner data. Both steps
     *     are error-isolated; exceptions are swallowed and only logged.
     */
    @Async
    public void syncAfterAddCar(Long carId,
                                String vin,
                                String phoneNumber,
                                String userIdHeader,
                                String timezone,
                                String acceptLanguage) {
        log.info("[pct-status-debug] afterAddCar async START | carId={}, vin={}, thread={}",
                carId, vin, Thread.currentThread().getName());

        // 1) Calculate + persist percentages (independent of Hyper).
        try {
            log.info("[pct-status-debug] afterAddCar calling executeServicePercentages (async) | carId={}", carId);
            carService.executeServicePercentages(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);
            log.info("[pct-status-debug] afterAddCar executeServicePercentages finished (async) | carId={}", carId);
        } catch (Exception e) {
            log.warn("[pct-status-debug] afterAddCar executeServicePercentages failed | carId={}, reason={}", carId, e.getMessage());
        }

        // 2) Pull Hyper history + apply partner data (idempotent, error-isolated).
        try {
            log.info("[pct-status-debug] afterAddCar calling Hyper sync (async) | carId={}, vin={}", carId, vin);
            visitHistoryService.getServiceHistoryByVin(vin, phoneNumber, userIdHeader, acceptLanguage);
            log.info("[pct-status-debug] afterAddCar Hyper sync finished (async) | carId={}, vin={}", carId, vin);
        } catch (Exception e) {
            log.warn("[pct-status-debug] afterAddCar Hyper sync failed | carId={}, vin={}, reason={}",
                    carId, vin, e.getMessage());
        }

        log.info("[pct-status-debug] afterAddCar async END | carId={}, vin={}", carId, vin);
    }
}
