package com.carland.carland_service.enums;

/**
 * tr: Slot rezervasyon modu. Staff range yaratırken seçer.
 * en: Slot booking mode. Staff chooses it when creating the range.
 */
public enum BookingMode {
    INSTANT,
    APPROVAL;

    public String apiValue() {
        return name().toLowerCase();
    }

    public static String normalizeOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return INSTANT.apiValue();
        }
        String value = raw.trim().toLowerCase();
        if (INSTANT.apiValue().equals(value) || APPROVAL.apiValue().equals(value)) {
            return value;
        }
        return null;
    }
}
