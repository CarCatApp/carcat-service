package com.carland.carland_service.idda;

import java.util.Locale;

/**
 * tr: Plaka karşılaştırma anahtarı. Büyük harf, boşluk, tire ve nokta atılır.
 *     77-BU-669 ile 77BU669 aynı plakadır.
 * en: Plate comparison key. Upper case, spaces, dashes and dots are removed.
 *     77-BU-669 and 77BU669 are the same plate.
 */
public final class IddaPlateKey {

    private IddaPlateKey() {
    }

    public static String of(String plate) {
        if (plate == null || plate.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(plate.length());
        for (int i = 0; i < plate.length(); i++) {
            char c = plate.charAt(i);
            if (c == ' ' || c == '-' || c == '.' || c == '\u00A0') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }
}
