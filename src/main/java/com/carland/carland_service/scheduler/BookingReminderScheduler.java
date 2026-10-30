package com.carland.carland_service.scheduler;

import com.carland.carland_service.config.BookingReminderProperties;
import com.carland.carland_service.service.BookingReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * tr: Elastiklik qədər bir tur. Sərhəd epoch-a oturur, ona görə 7 və ya 25 dəqiqə də eyni pəncərəni bir dəfə tutur.
 *     Dəyişiklik restart istəyir: növbəti tur açılışda qeyd olunur.
 * en: One tick per slack. Boundaries sit on the epoch, so 7 or 25 minutes still hit each window once.
 *     A change needs a restart: the next tick is registered at startup.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BookingReminderProperties.class)
@RequiredArgsConstructor
public class BookingReminderScheduler implements SchedulingConfigurer {

    private final BookingReminderProperties properties;
    private final BookingReminderService reminderService;
    private final Clock clock;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        int configured = properties.getSlackMinutes();
        int resolved = properties.resolvedSlackMinutes();
        if (configured != resolved) {
            log.warn("booking.reminder.slack-minutes {} is outside 1-{}, using {}",
                    configured, BookingReminderProperties.MAX_SLACK_MINUTES, resolved);
        } else {
            log.info("booking reminder slack {} minutes", resolved);
        }
        AtomicReference<Instant> planned = new AtomicReference<>();
        registrar.addTriggerTask(
                () -> {
                    Instant tick = planned.get();
                    if (tick != null) {
                        reminderService.sendDue(tick);
                    }
                },
                context -> {
                    Instant next = BookingReminderService.nextBoundary(clock.instant(), properties.slack());
                    planned.set(next);
                    return next;
                });
    }
}
