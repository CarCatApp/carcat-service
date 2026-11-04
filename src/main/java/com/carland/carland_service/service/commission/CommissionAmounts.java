package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.Visit;

import java.math.BigDecimal;
import java.util.List;

/**
 * tr: Panel ziyareti (booking_id) varsa tutar odur. Yoksa aynı gün ve aynı partnerin Hyper ziyaretleri toplanır.
 *     Aynı güne birden fazla randevu düşerse Hyper tutarı bölünmez, satır bekler.
 * en: A panel visit (booking_id) supplies the amount. Otherwise Hyper visits of that day and partner are summed.
 *     Two bookings on the same day do not split a Hyper amount; the row stays pending.
 */
public final class CommissionAmounts {

    private CommissionAmounts() {
    }

    public static CommissionAmountResult resolve(List<Visit> bookingVisits, List<Visit> hyperDayVisits, boolean ambiguousDay) {
        if (bookingVisits != null && !bookingVisits.isEmpty()) {
            BigDecimal panel = sum(bookingVisits);
            if (panel == null) {
                return CommissionAmountResult.pending();
            }
            return CommissionAmountResult.ready(panel);
        }
        if (ambiguousDay) {
            return CommissionAmountResult.pending();
        }
        BigDecimal hyper = sum(hyperDayVisits);
        if (hyper == null) {
            return CommissionAmountResult.pending();
        }
        return CommissionAmountResult.ready(hyper);
    }

    /**
     * tr: finalCost varsa o, yoksa cost. Hepsi boşsa null (bekler). Sıfır gerçek tutardır.
     * en: finalCost when present, otherwise cost. All empty is null (pending). Zero is a real amount.
     */
    static BigDecimal sum(List<Visit> visits) {
        if (visits == null || visits.isEmpty()) {
            return null;
        }
        boolean any = false;
        BigDecimal total = BigDecimal.ZERO;
        for (Visit visit : visits) {
            if (visit == null) {
                continue;
            }
            BigDecimal amount = visit.getFinalCostAmount() != null ? visit.getFinalCostAmount() : visit.getCostAmount();
            if (amount == null) {
                continue;
            }
            any = true;
            total = total.add(amount);
        }
        return any ? CommissionMoney.scale(total) : null;
    }
}
