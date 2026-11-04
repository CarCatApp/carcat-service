package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * tr: Komisyon saatleri Bakü. Metin 24 saat, HH:mm.
 * en: Commission clock is Baku. Text is 24-hour, HH:mm.
 */
public final class CommissionClock {

    public static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private CommissionClock() {
    }

    public static LocalDate bakuDate(OffsetDateTime time) {
        OffsetDateTime value = time == null ? OffsetDateTime.now(BAKU) : time;
        return value.atZoneSameInstant(BAKU).toLocalDate();
    }

    public static OffsetDateTime start(LocalDate day) {
        return day.atStartOfDay(BAKU).toOffsetDateTime();
    }

    public static String dateTime(OffsetDateTime time) {
        if (time == null) {
            return "";
        }
        return DATE_TIME.format(time.atZoneSameInstant(BAKU));
    }

    public static String date(LocalDate day) {
        if (day == null) {
            return "";
        }
        return DATE.format(day);
    }
}
