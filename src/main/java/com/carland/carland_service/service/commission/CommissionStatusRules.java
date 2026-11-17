package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * tr: Uğurlu Hyper: yox → new; son xidmət ≤365 → old; >365 → reactivated.
 *     Uğursuz Hyper/tx → failed. VIN yox / işlənməz → —.
 *     365-ci gün köhnədir. Müqayisə növbə / yoxlama anınadır. Gələcək tarix köhnə + əl yoxlama.
 * en: Successful Hyper: absent → new; last service ≤365 → old; >365 → reactivated.
 *     Failed Hyper/tx → failed. No/unusable VIN → —.
 *     Day 365 counts as old. Compared at booking/check time. Future date stays old and needs review.
 */
public final class CommissionStatusRules {

    public static final int LOYALTY_DAYS = 365;

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE_365 = "INACTIVE_365";
    /** tr: Köhnə sətirlər / testlər. en: Legacy rows / tests. */
    public static final String INACTIVE_12M = "INACTIVE_12M";
    public static final String NO_DATE = "NO_DATE";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String NO_RESPONSE = "NO_RESPONSE";
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
        LocalDate last = snapshot.dates().stream()
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (last == null) {
            return new CommissionDecision(CommissionStatus.FAILED, NO_DATE, null, detail);
        }
        OffsetDateTime created = bookingTime == null ? OffsetDateTime.now(CommissionClock.BAKU) : bookingTime;
        LocalDate day = CommissionClock.bakuDate(created);
        if (last.isAfter(day)) {
            return new CommissionDecision(CommissionStatus.OLD, FUTURE_DATE, last, detail);
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
        if (NO_DATE.equals(outcome)) {
            return "Son xidmət tarixi yoxdur";
        }
        if (INACTIVE_365.equals(outcome) || INACTIVE_12M.equals(outcome)) {
            return "Son xidmətdən 365 gündən çox keçib";
        }
        if (FUTURE_DATE.equals(outcome)) {
            return "Gələcək xidmət tarixi";
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
