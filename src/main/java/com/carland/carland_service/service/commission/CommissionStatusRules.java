package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * tr: Son xidmət 12 təqvim ayından təzədirsə köhnə. Qalan hallar yeni. Düz 12 ay yeni sayılır.
 *     Müqayisə növbənin yarandığı ana görədir. Gələcək tarix köhnə qalır və əl ilə yoxlanır.
 * en: A last service newer than 12 calendar months is old. Every other case is new. Exactly 12 months is new.
 *     The comparison uses the booking's creation instant. A future date stays old and is flagged for review.
 */
public final class CommissionStatusRules {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE_12M = "INACTIVE_12M";
    public static final String NO_DATE = "NO_DATE";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String NO_RESPONSE = "NO_RESPONSE";
    public static final String FUTURE_DATE = "FUTURE_DATE";
    public static final String ALREADY_NEW = "ALREADY_NEW";
    public static final String INVALID_VIN = "INVALID_VIN";

    /** tr: Köhnə sətirlər. en: Rows written before the 12-month rule. */
    public static final String FOUND = "found";
    public static final String LEGACY_NOT_FOUND = "not_found";
    public static final String TIMEOUT = "timeout";
    public static final String NO_VIN = "no_vin";

    private CommissionStatusRules() {
    }

    public static CommissionDecision decide(OffsetDateTime bookingTime, HyperVinSnapshot snapshot) {
        String detail = snapshot == null ? "failed" : snapshot.detail();
        if (snapshot == null || snapshot.kind() == HyperVinSnapshot.Kind.FAILED) {
            return new CommissionDecision(CommissionStatus.NEW, NO_RESPONSE, null, detail);
        }
        if (snapshot.kind() == HyperVinSnapshot.Kind.NOT_FOUND) {
            return new CommissionDecision(CommissionStatus.NEW, NOT_FOUND, null, detail);
        }
        LocalDate last = snapshot.dates().stream()
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (last == null) {
            return new CommissionDecision(CommissionStatus.NEW, NO_DATE, null, detail);
        }
        OffsetDateTime created = bookingTime == null ? OffsetDateTime.now(CommissionClock.BAKU) : bookingTime;
        if (last.isAfter(CommissionClock.bakuDate(created))) {
            return new CommissionDecision(CommissionStatus.OLD, FUTURE_DATE, last, detail);
        }
        OffsetDateTime due = last.plusMonths(12).atStartOfDay(CommissionClock.BAKU).toOffsetDateTime();
        if (!created.isBefore(due)) {
            return new CommissionDecision(CommissionStatus.NEW, INACTIVE_12M, last, detail);
        }
        return new CommissionDecision(CommissionStatus.OLD, ACTIVE, last, detail);
    }

    public static String outcomeLabel(String outcome) {
        if (ACTIVE.equals(outcome)) {
            return "Son 12 ayda xidmət var";
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
        if (INACTIVE_12M.equals(outcome)) {
            return "Son xidmətdən 12 ay keçib";
        }
        if (FUTURE_DATE.equals(outcome)) {
            return "Gələcək xidmət tarixi";
        }
        if (ALREADY_NEW.equals(outcome)) {
            return "Artıq yeni müştəri";
        }
        if (INVALID_VIN.equals(outcome)) {
            return "BAN 17 simvol deyil";
        }
        if (NO_VIN.equals(outcome)) {
            return "BAN yoxdur";
        }
        return "Hyper cavab vermədi";
    }
}
