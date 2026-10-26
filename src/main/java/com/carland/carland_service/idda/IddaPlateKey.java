package com.carland.carland_service.idda;

import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.InvalidStatusException;

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

    /**
     * tr: Yan yana iki A. Kiril ve Yunan A da Latin A sayılır. Ayırıcılar düşer: 10-AA-000, 10 AA 000.
     * en: Two adjacent A's. Cyrillic and Greek A count as Latin A. Separators drop: 10-AA-000, 10 AA 000.
     */
    public static boolean hasAdjacentAa(String plate) {
        if (plate == null || plate.isBlank()) {
            return false;
        }
        StringBuilder sb = new StringBuilder(plate.length());
        for (int i = 0; i < plate.length(); i++) {
            char folded = foldPlateChar(plate.charAt(i));
            if (folded != 0) {
                sb.append(folded);
            }
        }
        return sb.indexOf("AA") >= 0;
    }

    /** tr: Varsa 400 ve AA mesajı. / en: Throws 400 with the AA message when the plate matches. */
    public static void rejectAdjacentAa(String plate, String acceptLanguage) {
        if (hasAdjacentAa(plate)) {
            throw new InvalidStatusException(MessagesLangValues.AA_PLATE_NOT_ALLOWED.getMessageByLang(acceptLanguage));
        }
    }

    private static char foldPlateChar(char c) {
        if (c == '\u0410' || c == '\u0430' || c == '\u0391' || c == '\u03B1' || c == '\uFF21' || c == '\uFF41') {
            return 'A';
        }
        if (Character.isLetterOrDigit(c)) {
            return Character.toUpperCase(c);
        }
        return 0;
    }
}
