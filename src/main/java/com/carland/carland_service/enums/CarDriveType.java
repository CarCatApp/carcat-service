package com.carland.carland_service.enums;

/**
 * tr: Kia çekiş tipi. Araç resminin altından bir kez seçilir.
 * en: Kia drive type. Chosen once under the car image.
 */
public enum CarDriveType {
    FWD,
    RWD,
    AWD;

    public static CarDriveType fromStored(String raw) {
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
