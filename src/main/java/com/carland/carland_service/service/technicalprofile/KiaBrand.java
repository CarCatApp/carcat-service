package com.carland.carland_service.service.technicalprofile;

import java.util.Locale;

/**
 * tr: Marka metni Kia içeriyorsa true. Baştaki ve sondaki boşluk fark etmez. Büyük-küçük harf bakılmaz.
 * en: True when the brand text contains Kia. Surrounding spaces do not matter. Case is ignored.
 */
public final class KiaBrand {

    private KiaBrand() {
    }

    public static boolean isKia(String brand) {
        return contains(brand, "kia");
    }

    public static boolean isHyundai(String brand) {
        return contains(brand, "hyundai");
    }

    public static boolean isBmw(String brand) {
        return contains(brand, "bmw");
    }

    private static boolean contains(String brand, String token) {
        if (brand == null || brand.isBlank()) {
            return false;
        }
        return brand.toLowerCase(Locale.ROOT).contains(token);
    }
}
