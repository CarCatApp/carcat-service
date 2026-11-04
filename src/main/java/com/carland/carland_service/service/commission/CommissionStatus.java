package com.carland.carland_service.service.commission;

import java.math.BigDecimal;

/**
 * tr: Müşteri durumu kodu. Yeni = ilk randevu anında Hyper'da eski kayıt yok. Eski = vardı.
 * en: Customer-status code. New = no earlier Hyper record at the first booking. Old = there was one.
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
