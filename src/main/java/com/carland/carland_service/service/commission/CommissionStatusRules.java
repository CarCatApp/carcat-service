package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * tr: Baz = növbə / yoxlama anı. Yalnız o gündən ƏVVƏLKI Hyper tarixlər prior-dır
 *     (eyni gün və sonrası sayılmır). Prior yox → new; ≤365 → old; >365 → reactivated.
 *     Hyper fail → failed. VIN yox → —.
 * en: Base = booking/check moment. Only Hyper dates STRICTLY BEFORE that day are prior
 *     (same day and later do not count). No prior → new; ≤365 → old; >365 → reactivated.
 *     Hyper fail → failed. No VIN → —.
 */
public final class CommissionStatusRules {

    public static final int LOYALTY_DAYS = 365;

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE_365 = "INACTIVE_365";
    /** tr: Köhnə sətirlər / testlər. en: Legacy rows / tests. */
    public static final String INACTIVE_12M = "INACTIVE_12M";
    public static final String NO_DATE = "NO_DATE";
    public static final String NOT_FOUND = "NOT_FOUND";
    /** tr: Hyper tapıldı amma booking günündən əvvəl tarix yox. en: Found but no date before booking day. */
    public static final String NO_PRIOR = "NO_PRIOR";
    public static final String NO_RESPONSE = "NO_RESPONSE";
    /** tr: Köhnə audit sətirləri. Status artıq buna görə old yazılmır. en: Legacy audit only. */
    public static final String FUTURE_DATE = "FUTURE_DATE";
    public static final String ALREADY_NEW = "ALREADY_NEW";
    public static final String ALREADY_REACTIVATED = "ALREADY_REACTIVATED";
    public static final String KEEP_OLD = "KEEP_OLD";
    public static final String INVALID_VIN = "INVALID_VIN";
    public static final String FOUND = "found";
    public static final String LEGACY_NOT_FOUND = "not_found";
    public static final String TIMEOUT = "timeout";
    public static final String NO_VIN = "no_vin";
    public static final String CALCULATE_FAILED = "CALCULATE_FAILED";
    public static final String NO_TX = "NO_TX";

    private CommissionStatusRules() {
    }

    public static CommissionDecision decide(OffsetDateTime bookingTime, HyperVinSnapshot snapshot) {
        String detail = snapshot == null ? "failed" : snapshot.detail();
        if (snapshot == null || snapshot.kind() == HyperVinSnapshot.Kind.FAILED) {
            return new CommissionDecision(CommissionStatus.FAILED, NO_RESPONSE, null, detail);
        }
        if (snapshot.kind() == HyperVinSnapshot.Kind.NOT_FOUND) {
            return new CommissionDecision(CommissionStatus.NEW, NOT_FOUND, null, detail);
        }
        OffsetDateTime created = bookingTime == null ? OffsetDateTime.now(CommissionClock.BAKU) : bookingTime;
        LocalDate day = CommissionClock.bakuDate(created);
        List<LocalDate> prior = snapshot.dates().stream()
                .filter(Objects::nonNull)
                .filter(d -> d.isBefore(day))
                .toList();
        if (prior.isEmpty()) {
            // Same-day / after-booking Hyper dates are ignored — not prior → new.
            return new CommissionDecision(CommissionStatus.NEW, NO_PRIOR, null, detail);
        }
        LocalDate last = prior.stream().max(LocalDate::compareTo).orElse(null);
        if (last == null) {
            return new CommissionDecision(CommissionStatus.FAILED, NO_DATE, null, detail);
        }
        long days = ChronoUnit.DAYS.between(last, day);
        if (days > LOYALTY_DAYS) {
            return new CommissionDecision(CommissionStatus.REACTIVATED, INACTIVE_365, last, detail);
        }
        return new CommissionDecision(CommissionStatus.OLD, ACTIVE, last, detail);
    }

    public static String outcomeLabel(String outcome) {
        if (ACTIVE.equals(outcome)) {
            return "Son 365 gündə xidmət var";
        }
        if (FOUND.equals(outcome)) {
            return "Hyper-da qeyd var";
        }
        if (NOT_FOUND.equals(outcome) || LEGACY_NOT_FOUND.equals(outcome)) {
            return "Hyper-da qeyd yoxdur";
        }
        if (NO_PRIOR.equals(outcome)) {
            return "Növbədən əvvəl Hyper xidməti yoxdur";
        }
        if (NO_DATE.equals(outcome)) {
            return "Son xidmət tarixi yoxdur";
        }
        if (INACTIVE_365.equals(outcome) || INACTIVE_12M.equals(outcome)) {
            return "Son xidmətdən 365 gündən çox keçib";
        }
        if (FUTURE_DATE.equals(outcome)) {
            return "Gələcək xidmət tarixi (köhnə qayda)";
        }
        if (ALREADY_NEW.equals(outcome)) {
            return "Artıq yeni müştəri";
        }
        if (ALREADY_REACTIVATED.equals(outcome)) {
            return "Artıq geri qazanılmış müştəri";
        }
        if (KEEP_OLD.equals(outcome)) {
            return "Köhnə status saxlanıldı";
        }
        if (INVALID_VIN.equals(outcome)) {
            return "BAN Hyper sorğusu üçün uyğun deyil";
        }
        if (NO_VIN.equals(outcome)) {
            return "BAN yoxdur";
        }
        if (CALCULATE_FAILED.equals(outcome) || NO_TX.equals(outcome)) {
            return "Status hesablana bilmədi";
        }
        return "Hyper cavab vermədi";
    }
}
