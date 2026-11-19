package com.carland.carland_service.enums;

/**
 * tr: Kia transmisyon varyantı. Transmission fluid tıklanınca bir kez seçilir.
 * en: Kia transmission variant. Chosen once when Transmission fluid is tapped.
 */
public enum CarTransmissionVariant {
    AT4,
    AT6_8,
    CVT_DCT,
    MANUAL;

    public static CarTransmissionVariant fromStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
