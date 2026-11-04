package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.CommissionRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * tr: Tamamlanma gününde yürürlükte olan satır. Sonraki tarihler sayılmaz. Satır yoksa 6 / 2.
 * en: The row in force on the completion day. Later dates do not count. No row means 6 / 2.
 */
public final class CommissionRates {

    private CommissionRates() {
    }

    public static BigDecimal pick(List<CommissionRate> rows, LocalDate day, BigDecimal fallback) {
        CommissionRate best = null;
        if (rows != null && day != null) {
            for (CommissionRate row : rows) {
                if (row == null || row.getEffectiveFrom() == null || row.getPercent() == null) {
                    continue;
                }
                if (row.getEffectiveFrom().isAfter(day)) {
                    continue;
                }
                if (best == null || newer(row, best)) {
                    best = row;
                }
            }
        }
        return best == null ? fallback : best.getPercent();
    }

    private static boolean newer(CommissionRate row, CommissionRate best) {
        int byDay = row.getEffectiveFrom().compareTo(best.getEffectiveFrom());
        if (byDay != 0) {
            return byDay > 0;
        }
        long rowId = row.getId() == null ? 0L : row.getId();
        long bestId = best.getId() == null ? 0L : best.getId();
        return rowId > bestId;
    }
}
