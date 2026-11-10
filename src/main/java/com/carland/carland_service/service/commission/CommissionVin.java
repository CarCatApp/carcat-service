package com.carland.carland_service.service.commission;

import com.carland.carland_service.hyper.HyperVin;

/**
 * tr: BAN komisyon anahtarı. Tire/boşluk düşer ({@link HyperVin#compact}).
 *     Hyper sorğusu kısa VIN'i sağdan 0 ile 17 yapar; 17 kontrolü pad sonrası.
 * en: BAN commission key. Drops dash/space ({@link HyperVin#compact}).
 *     Hyper query right-pads a short VIN with 0 to 17; the 17 check is after padding.
 */
public final class CommissionVin {

    private CommissionVin() {
    }

    public static String normalize(String vin) {
        return HyperVin.compact(vin);
    }

    /** tr: Hyper'a sorulabilir (pad sonrası 17). / en: Hyper-queryable (17 after pad). */
    public static boolean seventeen(String normalized) {
        return HyperVin.hyperQueryable(normalized);
    }
}
