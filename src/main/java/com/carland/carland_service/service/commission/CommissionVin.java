package com.carland.carland_service.service.commission;

import java.util.Locale;

/**
 * tr: BAN komisyon anahtarı. Boşluklar silinir, böyük harf. Hyper sorğusu 17 simvol ister.
 * en: BAN commission key. Spaces are removed, then upper case. The Hyper query requires 17 characters.
 */
public final class CommissionVin {

    private CommissionVin() {
    }

    public static String normalize(String vin) {
        if (vin == null) {
            return null;
        }
        String value = vin.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    public static boolean seventeen(String normalized) {
        return normalized != null && normalized.length() == 17;
    }
}
