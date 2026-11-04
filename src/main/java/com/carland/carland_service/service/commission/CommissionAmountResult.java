package com.carland.carland_service.service.commission;

import java.math.BigDecimal;

/**
 * tr: Tutar hazırsa komisyona girer. Hazır değilse satır bekler ve toplama girmez.
 * en: A ready amount enters the commission. Otherwise the row waits and stays out of the totals.
 */
public record CommissionAmountResult(boolean ready, BigDecimal serviceAmount) {

    public static CommissionAmountResult pending() {
        return new CommissionAmountResult(false, null);
    }

    public static CommissionAmountResult ready(BigDecimal amount) {
        return new CommissionAmountResult(true, CommissionMoney.scale(amount));
    }
}
