package com.carland.carland_service.service;

import java.util.Locale;
import java.util.Set;

/**
 * tr: Şimdilik iki promo kodu. Karşılaştırma büyük/küçük harf duymaz.
 * en: Two promo codes for now. Comparison ignores case.
 */
public final class PromoCodes {

    public static final String INMERGE_2026 = "inmerge2026";
    public static final String CARCAT_2026 = "carcat2026";

    private static final Set<String> CODES = Set.of(INMERGE_2026, CARCAT_2026);

    private PromoCodes() {
    }

    public static boolean matches(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return CODES.contains(raw.trim().toLowerCase(Locale.ROOT));
    }
}
