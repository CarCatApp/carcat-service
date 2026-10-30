package com.carland.carland_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * tr: Növbə xatırlatmasının elastik pəncərəsi. Cron dövrü ilə eyni dəyərdir. 1–60 dəqiqə.
 * en: Elastic window for booking reminders. The cron period uses the same value. 1–60 minutes.
 */
@Data
@ConfigurationProperties(prefix = "booking.reminder")
public class BookingReminderProperties {

    public static final int DEFAULT_SLACK_MINUTES = 10;
    public static final int MAX_SLACK_MINUTES = 60;

    private int slackMinutes = DEFAULT_SLACK_MINUTES;

    /**
     * tr: Aralık dışındaki dəyər 10 dəqiqəyə düşür. 24 saat və 3 saat pəncərələri üst-üstə düşməsin.
     * en: A value outside the range falls back to 10 minutes so the 24h and 3h windows stay apart.
     */
    public int resolvedSlackMinutes() {
        if (slackMinutes < 1 || slackMinutes > MAX_SLACK_MINUTES) {
            return DEFAULT_SLACK_MINUTES;
        }
        return slackMinutes;
    }

    public Duration slack() {
        return Duration.ofMinutes(resolvedSlackMinutes());
    }
}
