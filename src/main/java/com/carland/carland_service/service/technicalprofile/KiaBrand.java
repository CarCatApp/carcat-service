package com.carland.carland_service.service.technicalprofile;

/**
 * tr: Marka adının Kia olup olmadığını bilen küçük yardımcı.
 * en: Tiny helper for whether the brand name is Kia.
 */
public final class KiaBrand {

    private KiaBrand() {
    }

    public static boolean isKia(String brand) {
        return brand != null && "kia".equalsIgnoreCase(brand.trim());
    }
}
