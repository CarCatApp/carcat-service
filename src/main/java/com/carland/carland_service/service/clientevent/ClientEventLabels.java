package com.carland.carland_service.service.clientevent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * tr: Admin mətnləri Azərbaycan dilində. API və DB dəyərləri ingiliscə qalır.
 *     Saat 24 saatdır, Bakı UTC+4 (yay vaxtı yoxdur), users səhifəsi ilə eyni.
 * en: Admin copy is Azerbaijani. API and DB values stay English.
 *     Time is 24-hour, Baku UTC+4 (no daylight saving), same as the users page.
 */
public final class ClientEventLabels {

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.ROOT);
    public static final DateTimeFormatter TIME_SECONDS = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss", Locale.ROOT);
    public static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);

    private ClientEventLabels() {
    }

    public static String time(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return TIME.format(utc.plusHours(4));
    }

    public static String baku(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return TIME_SECONDS.format(utc.plusHours(4));
    }

    public static String iso(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return utc.atOffset(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static String dayKey(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return utc.plusHours(4).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String dayLabel(String isoDay) {
        try {
            return DAY.format(java.time.LocalDate.parse(isoDay));
        } catch (DateTimeParseException ex) {
            return isoDay;
        }
    }

    public static String type(String eventType) {
        if (eventType == null) {
            return "";
        }
        return switch (eventType) {
            case "fatal" -> "Çökmə";
            case "nonFatal" -> "Non-fatal";
            case "apiError" -> "API xətası";
            default -> eventType;
        };
    }

    public static String issueStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "new" -> "Yeni";
            case "ongoing" -> "Davam edir";
            case "resolved" -> "Həll olunub";
            default -> status;
        };
    }

    public static String network(String network) {
        if (network == null || network.isBlank()) {
            return "";
        }
        return switch (network) {
            case "wifi" -> "Wi-Fi";
            case "mobile" -> "Mobil";
            case "none" -> "Şəbəkə yoxdur";
            case "unknown" -> "Naməlum";
            default -> network;
        };
    }

    public static String os(String name) {
        if (name == null) {
            return "";
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "ios" -> "iOS";
            case "android" -> "Android";
            default -> name;
        };
    }

    public static String statusCode(Integer status) {
        if (status == null) {
            return "Bağlantı";
        }
        return Integer.toString(status);
    }

    public static String user(Long userId) {
        if (userId == null) {
            return "Qonaq";
        }
        return Long.toString(userId);
    }

    public static String memory(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "";
        }
        long mb = bytes / (1024L * 1024L);
        return mb + " MB";
    }

    public static String crumbTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return time(LocalDateTime.ofInstant(java.time.Instant.parse(raw.trim()), java.time.ZoneOffset.UTC));
        } catch (DateTimeParseException ex) {
            try {
                return time(LocalDateTime.ofInstant(
                        java.time.OffsetDateTime.parse(raw.trim()).toInstant(), java.time.ZoneOffset.UTC));
            } catch (DateTimeParseException ignored) {
                return raw;
            }
        }
    }
}
