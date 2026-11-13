package com.carland.carland_service.util;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * tr: Admin panel tarih gösterimi (Asia/Baku, +4).
 * en: Admin panel date display (Asia/Baku, +4).
 */
public final class AdminDateFormats {

    private static final String[] AZ_MONTHS = {
            "Yan", "Fev", "Mar", "Apr", "May", "İyn",
            "İyl", "Avq", "Sen", "Okt", "Noy", "Dek"
    };

    private AdminDateFormats() {
    }

    /**
     * tr: Server/UTC LocalDateTime → Baku metni, örn. {@code 13 Sen 2026 13:52}. Null ise boş.
     * en: Server/UTC LocalDateTime → Baku text, e.g. {@code 13 Sen 2026 13:52}. Null → empty.
     */
    public static String formatBaku(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }
        LocalDateTime baku = createdAt.plusHours(4);
        return String.format(Locale.ROOT, "%d %s %d %02d:%02d",
                baku.getDayOfMonth(),
                AZ_MONTHS[baku.getMonthValue() - 1],
                baku.getYear(),
                baku.getHour(),
                baku.getMinute());
    }
}
