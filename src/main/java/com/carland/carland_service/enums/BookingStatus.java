package com.carland.carland_service.enums;

import java.util.List;

/**
 * tr: Owner-app booking satır durumu.
 * en: Owner-app booking row status.
 */
public enum BookingStatus {
    PENDING,
    /** Staff accepted an approval booking. */
    CONFIRMED,
    /** Instant slot: no staff click; auto-accepted on create. */
    AUTO_ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public String apiValue() {
        return name().toLowerCase();
    }

    /** Occupies range capacity until cancelled/rejected/completed. */
    public static List<String> occupyingCapacity() {
        return List.of(PENDING.apiValue(), CONFIRMED.apiValue(), AUTO_ACCEPTED.apiValue());
    }
}
