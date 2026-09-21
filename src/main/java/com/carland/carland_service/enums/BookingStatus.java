package com.carland.carland_service.enums;

/**
 * tr: Owner-app booking satır durumu.
 * en: Owner-app booking row status.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public String apiValue() {
        return name().toLowerCase();
    }
}
