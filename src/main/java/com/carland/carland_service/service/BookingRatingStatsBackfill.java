package com.carland.carland_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * tr: Eski satırları bir kez kolonlara yazar; sonraki GET hesaplamaz.
 * en: One-shot write of stored averages so GET does not aggregate.
 */
@Slf4j
@Component
@Order(25)
@RequiredArgsConstructor
public class BookingRatingStatsBackfill implements ApplicationRunner {

    private final BookingRatingService bookingRatingService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            bookingRatingService.recalculateStoredStats();
        } catch (Exception ex) {
            log.warn("BOOKING_RATING_BACKFILL_FAIL {}", ex.toString());
        }
    }
}
