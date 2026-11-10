package com.carland.carland_service.hyper;

import java.util.Locale;

/**
 * tr: Hyper / DB VIN köprüsü. {@code cars.vin} yazılmaz.
 *     Tireli kısa VIN ({@code C11-437527}), tire'siz ({@code C11437527}) ve Hyper'ın
 *     17+sağdan 8 sıfır formu ({@code C1143752700000000}) aynı anahtara iner.
 * en: Hyper / DB VIN bridge. Does not rewrite {@code cars.vin}.
 *     Dashed short ({@code C11-437527}), compact ({@code C11437527}) and Hyper's
 *     17-with-eight-trailing-zeros form ({@code C1143752700000000}) share one key.
 */
public final class HyperVin {

    private static final int HYPER_LEN = 17;
    private static final String EIGHT_ZEROS = "00000000";

    private HyperVin() {
    }

    /**
     * tr: DB / komisyon eşleme anahtarı. Tire-boşluk düşer; 17 ve son 8 sıfırsa trailing 0 silinir.
     * en: DB / commission match key. Drops dash/space; strips trailing zeros when length 17 and last 8 are 0.
     */
    public static String vinKey(String vin) {
        return forDbLookup(vin);
    }

    /**
     * tr: Tire ve boşluk düşer, büyük harf. Hyper GET path'i için 17'den kısaysa sağa 0.
     * en: Drops dashes and spaces, upper case. Right-pads with 0 when shorter than 17 for Hyper GET.
     */
    public static String forHyperApi(String vin) {
        String compact = compact(vin);
        if (compact == null) {
            return null;
        }
        if (compact.length() < HYPER_LEN) {
            return compact + "0".repeat(HYPER_LEN - compact.length());
        }
        return compact;
    }

    /**
     * tr: Webhook/DB arama: compact + (17 ve son 8 sıfır ise) trailing 0 sil.
     * en: Webhook/DB lookup: compact, then strip trailing zeros when length 17 and last 8 are zero.
     */
    public static String forDbLookup(String vin) {
        String compact = compact(vin);
        if (compact == null) {
            return null;
        }
        if (compact.length() == HYPER_LEN && compact.endsWith(EIGHT_ZEROS)) {
            int end = compact.length();
            while (end > 0 && compact.charAt(end - 1) == '0') {
                end--;
            }
            return end == 0 ? compact : compact.substring(0, end);
        }
        return compact;
    }

    /**
     * tr: Hyper'a sorulabilir mi — pad sonrası 17 simvol.
     * en: Whether Hyper can be queried — length 17 after padding.
     */
    public static boolean hyperQueryable(String vin) {
        String hyper = forHyperApi(vin);
        return hyper != null && hyper.length() == HYPER_LEN;
    }

    /** tr: Tire, boşluk, NBSP düşer; büyük harf. / en: Drops dash, space, NBSP; upper case. */
    public static String compact(String vin) {
        if (vin == null || vin.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(vin.length());
        for (int i = 0; i < vin.length(); i++) {
            char c = vin.charAt(i);
            if (c == '-' || c == ' ' || c == '\u00A0') {
                continue;
            }
            sb.append(c);
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }
}
