package com.carland.carland_service.service.commission;

import java.math.BigDecimal;

/**
 * tr: Müştəri statusu (Hyper loyalty).
 *     new / reactivated = 6% və yapışır. old = 2%; yalnız old → reactivated ola bilər.
 * en: Customer status (Hyper loyalty).
 *     new / reactivated = 6% and sticky. old = 2%; only old may become reactivated.
 */
public final class CommissionStatus {

    public static final String NEW = "new";
    public static final String REACTIVATED = "reactivated";
    public static final String OLD = "old";

    public static final BigDecimal NEW_PERCENT = new BigDecimal("6");
    public static final BigDecimal OLD_PERCENT = new BigDecimal("2");

    private CommissionStatus() {
    }

    public static boolean known(String status) {
        return NEW.equals(status) || REACTIVATED.equals(status) || OLD.equals(status);
    }

    /** tr: Yeni və geri qazanılmış bir daha dəyişmir. en: New and reactivated never change again. */
    public static boolean sticky(String status) {
        return NEW.equals(status) || REACTIVATED.equals(status);
    }

    public static BigDecimal fallback(String status) {
        return OLD.equals(status) ? OLD_PERCENT : NEW_PERCENT;
    }

    public static String label(String status) {
        if (OLD.equals(status)) {
            return "Köhnə müştəri";
        }
        if (REACTIVATED.equals(status)) {
            return "Geri qazanılmış müştəri";
        }
        return "Yeni müştəri";
    }

    /** tr: Admin Cars hyperLoyalty qısa yazı. en: Short admin Cars hyperLoyalty label. */
    public static String loyaltyLabel(String status) {
        if (OLD.equals(status)) {
            return "köhnə";
        }
        if (REACTIVATED.equals(status)) {
            return "geri qazanılmış";
        }
        if (NEW.equals(status)) {
            return "yeni";
        }
        return "—";
    }
}
