package com.carland.carland_service.service.commission;

/**
 * tr: BAN komisyon anahtarı. bookings.vin, büyük harf, boşluksuz.
 * en: BAN commission key. bookings.vin, upper case, trimmed.
 */
public final class CommissionVin {

    private CommissionVin() {
    }

    public static String normalize(String vin) {
        if (vin == null) {
            return null;
        }
        String value = vin.trim().toUpperCase();
        return value.isEmpty() ? null : value;
    }
}
