package com.carland.carland_service.service.commission;

import java.math.BigDecimal;

/**
 * tr: Müştəri statusu. Yeni = son 12 ayda Hyper xidməti yoxdur. Köhnə = var.
 * en: Customer status. New = no Hyper service in the last 12 months. Old = there was one.
 */
public final class CommissionStatus {

    public static final String NEW = "new";
    public static final String OLD = "old";

    public static final BigDecimal NEW_PERCENT = new BigDecimal("6");
    public static final BigDecimal OLD_PERCENT = new BigDecimal("2");

    private CommissionStatus() {
    }

    public static boolean known(String status) {
        return NEW.equals(status) || OLD.equals(status);
    }

    public static java.math.BigDecimal fallback(String status) {
        return OLD.equals(status) ? OLD_PERCENT : NEW_PERCENT;
    }

    public static String label(String status) {
        return OLD.equals(status) ? "Köhnə müştəri" : "Yeni müştəri";
    }
}
