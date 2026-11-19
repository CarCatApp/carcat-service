package com.carland.carland_service.enums;

/**
 * tr: Dişli kayış mı zincir mi. Timing belt yüzdesine tıklanınca bir kez seçilir (tüm markalar).
 * en: Timing belt vs chain. Chosen once when the timing-belt percentage is tapped (all brands).
 */
public enum TimingBeltChoice {
    BELT,
    CHAIN;

    public static TimingBeltChoice fromStored(String raw) {
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
