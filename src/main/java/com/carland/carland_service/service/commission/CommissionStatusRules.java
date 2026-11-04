package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.util.Objects;

/**
 * tr: Hyper günü randevunun Bakü gününden önceyse eski. Aynı gün ve sonrası yeni.
 *     Cevap yoksa veya kayıt yoksa yeni. Saat alanı gelene kadar aynı gün eski sayılmaz.
 * en: A Hyper date before the booking's Baku day is old. The same day and after is new.
 *     No answer or no record is new. The same day is not old until Hyper sends a clock.
 */
public final class CommissionStatusRules {

    public static final String FOUND = "found";
    public static final String NOT_FOUND = "not_found";
    public static final String TIMEOUT = "timeout";
    public static final String NO_VIN = "no_vin";

    private CommissionStatusRules() {
    }

    public static CommissionDecision decide(LocalDate bookingDay, HyperVinSnapshot snapshot) {
        if (snapshot == null || snapshot.kind() == HyperVinSnapshot.Kind.FAILED) {
            String detail = snapshot == null ? "failed" : snapshot.detail();
            return new CommissionDecision(CommissionStatus.NEW, TIMEOUT, null, detail);
        }
        if (snapshot.kind() == HyperVinSnapshot.Kind.NOT_FOUND) {
            return new CommissionDecision(CommissionStatus.NEW, NOT_FOUND, null, snapshot.detail());
        }
        LocalDate earliest = snapshot.dates().stream()
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
        if (earliest != null && bookingDay != null && earliest.isBefore(bookingDay)) {
            return new CommissionDecision(CommissionStatus.OLD, FOUND, earliest, snapshot.detail());
        }
        return new CommissionDecision(CommissionStatus.NEW, FOUND, earliest, snapshot.detail());
    }

    public static String outcomeLabel(String outcome) {
        if (FOUND.equals(outcome)) {
            return "Hyper-da qeyd var";
        }
        if (NOT_FOUND.equals(outcome)) {
            return "Hyper-da qeyd yoxdur";
        }
        if (NO_VIN.equals(outcome)) {
            return "BAN yoxdur";
        }
        return "Hyper cavab vermədi";
    }
}
