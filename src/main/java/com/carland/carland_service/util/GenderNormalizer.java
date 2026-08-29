package com.carland.carland_service.util;

import java.util.Locale;

/**
 * tr: SIMA ve admin filtrelerinde cinsiyeti MALE / FEMALE olarak normalize eder.
 * en: Normalizes gender to MALE / FEMALE for SIMA profile copy and admin filters.
 */
public final class GenderNormalizer {

    private GenderNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "M", "MALE", "MAN" -> "MALE";
            case "F", "FEMALE", "WOMAN" -> "FEMALE";
            default -> s.length() <= 16 ? s : s.substring(0, 16);
        };
    }

    /**
     * tr: Admin filtresi: boş = all; aksi halde yalnız MALE veya FEMALE.
     * en: Admin filter: blank = all; otherwise only MALE or FEMALE.
     */
    public static String filterOrNull(String raw) {
        String n = normalize(raw);
        if (n == null) {
            return null;
        }
        if ("MALE".equals(n) || "FEMALE".equals(n)) {
            return n;
        }
        throw new IllegalArgumentException("Gender must be MALE or FEMALE");
    }
}
