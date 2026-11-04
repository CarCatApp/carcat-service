package com.carland.carland_service.service.commission;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * tr: Komisyon = hizmet bedeli × faiz, 2 ondalık, yarım yukarı.
 * en: Commission = service amount × percent, 2 decimals, half up.
 */
public final class CommissionMoney {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private CommissionMoney() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal commission(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) {
            return null;
        }
        return scale(amount).multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static String azn(BigDecimal amount) {
        BigDecimal scaled = scale(amount);
        if (scaled == null) {
            return "0,00 AZN";
        }
        return scaled.toPlainString().replace('.', ',') + " AZN";
    }

    public static String percent(BigDecimal rate) {
        if (rate == null) {
            return "";
        }
        return rate.stripTrailingZeros().toPlainString().replace('.', ',') + "%";
    }
}
