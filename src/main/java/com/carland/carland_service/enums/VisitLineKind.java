package com.carland.carland_service.enums;

import java.util.Locale;

/**
 * tr: Panelden yazılan ziyaret satırının növü.
 * en: Kind of a visit line written by the partner panel.
 */
public enum VisitLineKind {
    /** Booking package line with periodic=true. Price is included in the package. */
    PACKAGE,
    /** Package line with periodic=false. Info only: no card, no price. */
    PACKAGE_INFO,
    /** Individual (fərdi) service: booked or added as "Dövri". */
    INDIVIDUAL,
    /** One-off (birdəfəlik) work typed by the operator. Shown with individual services on mobile. */
    ONE_TIME,
    /** One-off repair opened from the booking's təmir/yoxlanış text. Mobile "Inspection & repair". */
    INSPECTION;

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lines that repeat and may carry next date / next km. */
    public boolean periodic() {
        return this == PACKAGE || this == INDIVIDUAL;
    }

    public static VisitLineKind fromApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
