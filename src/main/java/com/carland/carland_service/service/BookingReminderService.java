package com.carland.carland_service.service;

import com.carland.carland_service.config.BookingReminderProperties;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * tr: Təsdiqli növbəyə 24 saat və 3 saat qalmış push. Pəncərə eni properties-dəki elastiklikdir.
 *     Göndərildi sahəsi yoxdur: eyni enlikdəki növbəti tur növbəni çölə atır.
 * en: Push for a confirmed booking about 24 hours and about 3 hours ahead. The window width is the
 *     slack from properties. There is no sent flag: the next tick of the same width leaves the booking out.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingReminderService {

    private static final Duration DAY = Duration.ofHours(24);
    private static final Duration SOON = Duration.ofHours(3);

    private final BookingReminderProperties properties;
    private final BookingRepository bookingRepository;
    private final BookingPushService bookingPushService;

    /**
     * tr: Planlanmış sərhəd. İndi tam sərhəddirsə növbəti sərhəd gəlir, açılışda dərhal göndərməsin.
     * en: The next epoch boundary. An exact boundary yields the following one so startup does not send immediately.
     */
    public static Instant nextBoundary(Instant now, Duration slack) {
        long step = slack.toSeconds();
        long epoch = now.getEpochSecond();
        long next = (Math.floorDiv(epoch, step) + 1) * step;
        return Instant.ofEpochSecond(next);
    }

    /**
     * tr: Mərkəz lead-dir, en slack-dir, sağ uc xaric. Məsələn 10 dəq: [2s 55d, 3s 05d).
     * en: Centered on the lead, slack wide, exclusive end. For 10 minutes: [2h 55m, 3h 05m).
     */
    public static ReminderWindow window(Instant tick, Duration lead, Duration slack) {
        Duration half = slack.dividedBy(2);
        Instant center = tick.plus(lead);
        return new ReminderWindow(
                OffsetDateTime.ofInstant(center.minus(half), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(center.plus(half), ZoneOffset.UTC));
    }

    public void sendDue(Instant tick) {
        Duration slack = properties.slack();
        int dayBefore = dispatch(window(tick, DAY, slack), true);
        int soon = dispatch(window(tick, SOON, slack), false);
        log.info("booking reminder tick={} slackMinutes={} in24h={} in3h={}",
                tick, slack.toMinutes(), dayBefore, soon);
    }

    private int dispatch(ReminderWindow window, boolean dayBefore) {
        List<Booking> rows = bookingRepository.findReadyForReminder(
                BookingStatus.occupyingCapacity(), window.fromInclusive(), window.toExclusive());
        int sent = 0;
        for (Booking booking : rows) {
            try {
                if (dayBefore) {
                    bookingPushService.remindDayBefore(booking);
                } else {
                    bookingPushService.remindSoon(booking);
                }
                sent++;
            } catch (RuntimeException ex) {
                log.warn("booking reminder failed id={} error={}",
                        booking.getId(), ex.getClass().getSimpleName());
            }
        }
        return sent;
    }

    public record ReminderWindow(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {}
}
