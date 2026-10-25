package com.carland.carland_service.util;

/**
 * tr: Azerbaycan plakasını görüntü formatına çevirir.
 *     Harf ve rakam dışı silinir, kalan büyük harfe çevrilir, tipe göre tek boşluk konur.
 *     Standart {@code 00 XX 000}, diplomatik {@code 021 D 091},
 *     yabancı {@code M 591 884}, motosiklet {@code 99 G 250}.
 *     Null ve bu dört kalıp dışı girdi değişmez. Aynı metin yeniden aynı sonucu verir.
 * en: Formats an Azerbaijan plate for display.
 *     Non-letters and non-digits are removed, the rest is uppercased, then one space is inserted by type.
 *     Standard {@code 00 XX 000}, diplomatic {@code 021 D 091},
 *     foreign {@code M 591 884}, motorcycle {@code 99 G 250}.
 *     Null and anything outside these four patterns is unchanged. Formatting again is a no-op.
 */
public final class PlateNumbers {

    private PlateNumbers() {
    }

    public static String format(String plate) {
        if (plate == null) {
            return null;
        }
        String cleaned = lettersAndDigitsUpper(plate);
        int n = cleaned.length();
        if (n == 0) {
            return plate;
        }
        if (Character.isLetter(cleaned.charAt(0))) {
            if (foreign(cleaned)) {
                return cleaned.charAt(0) + " " + cleaned.substring(1, 4) + " " + cleaned.substring(4);
            }
            return plate;
        }
        if (n >= 4 && Character.isLetter(cleaned.charAt(2)) && Character.isDigit(cleaned.charAt(3))) {
            if (motorcycle(cleaned)) {
                return cleaned.substring(0, 2) + " " + cleaned.charAt(2) + " " + cleaned.substring(3);
            }
            return plate;
        }
        if (n >= 4 && Character.isLetter(cleaned.charAt(2)) && Character.isLetter(cleaned.charAt(3))) {
            if (standard(cleaned)) {
                return cleaned.substring(0, 2) + " " + cleaned.substring(2, 4) + " " + cleaned.substring(4);
            }
            return plate;
        }
        if (n >= 3 && Character.isDigit(cleaned.charAt(2))) {
            if (diplomatic(cleaned)) {
                return cleaned.substring(0, 3) + " " + cleaned.charAt(3) + " " + cleaned.substring(4);
            }
            return plate;
        }
        return plate;
    }

    private static String lettersAndDigitsUpper(String plate) {
        StringBuilder sb = new StringBuilder(plate.length());
        for (int i = 0; i < plate.length(); i++) {
            char c = plate.charAt(i);
            if (Character.isLetter(c) || Character.isDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    /** 1 harf + 3 rakam + 3 rakam. */
    private static boolean foreign(String s) {
        return s.length() == 7
                && Character.isLetter(s.charAt(0))
                && digits(s, 1, 7);
    }

    /** 2 rakam + 1 harf + 3 rakam. */
    private static boolean motorcycle(String s) {
        return s.length() == 6
                && digits(s, 0, 2)
                && Character.isLetter(s.charAt(2))
                && digits(s, 3, 6);
    }

    /** 2 rakam + 2 harf + 3 rakam. */
    private static boolean standard(String s) {
        return s.length() == 7
                && digits(s, 0, 2)
                && Character.isLetter(s.charAt(2))
                && Character.isLetter(s.charAt(3))
                && digits(s, 4, 7);
    }

    /** 3 rakam + 1 harf + 3 rakam. */
    private static boolean diplomatic(String s) {
        return s.length() == 7
                && digits(s, 0, 3)
                && Character.isLetter(s.charAt(3))
                && digits(s, 4, 7);
    }

    private static boolean digits(String s, int from, int to) {
        for (int i = from; i < to; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
