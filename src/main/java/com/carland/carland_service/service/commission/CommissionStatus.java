package com.carland.carland_service.service.commission;

import java.math.BigDecimal;

/**
 * tr: Müştəri statusu (Hyper loyalty).
 *     new / reactivated / old yalnız Hyper uğurlu olanda yazılır.
 *     new / reactivated yapışır. old yenidən yoxlanır; yalnız reactivated ola bilər.
 *     failed / — uğursuz hesablama; sticky deyil, UI-da görünür.
 * en: Customer status (Hyper loyalty).
 *     new / reactivated / old only after a successful Hyper read.
 *     new / reactivated stick. old is re-checked and may become reactivated only.
 *     failed / — means calculate failed; not sticky; shown in UI.
 */
public final class CommissionStatus {

    public static final String NEW = "new";
    public static final String REACTIVATED = "reactivated";
    public static final String OLD = "old";
    /** tr: Hyper/tx uğursuz. en: Hyper/tx failure. */
    public static final String FAILED = "failed";
    /** tr: VIN yox / işlənə bilməz. en: No VIN / cannot process. */
    public static final String DASH = "-";

    public static final BigDecimal NEW_PERCENT = new BigDecimal("6");
    public static final BigDecimal OLD_PERCENT = new BigDecimal("2");

    private CommissionStatus() {
    }

    /** tr: Uğurlu loyalty kodları. en: Successful loyalty codes. */
    public static boolean success(String status) {
        return NEW.equals(status) || REACTIVATED.equals(status) || OLD.equals(status);
    }

    /** tr: UI / cədvəldə saxlanılan bütün kodlar. en: Every code we persist for UI. */
    public static boolean known(String status) {
        return success(status) || FAILED.equals(status) || DASH.equals(status);
    }

    /** tr: Yeni və geri qazanılmış bir daha dəyişmir. en: New and reactivated never change again. */
    public static boolean sticky(String status) {
        return NEW.equals(status) || REACTIVATED.equals(status);
    }

    public static BigDecimal fallback(String status) {
        return OLD.equals(status) ? OLD_PERCENT : NEW_PERCENT;
    }

    public static String label(String status) {
        if (OLD.equals(status)) {
            return "Köhnə müştəri";
        }
        if (REACTIVATED.equals(status)) {
            return "Geri qazanılmış müştəri";
        }
        if (FAILED.equals(status)) {
            return "failed";
        }
        if (DASH.equals(status)) {
            return "—";
        }
        if (NEW.equals(status)) {
            return "Yeni müştəri";
        }
        return "failed";
    }

    /** tr: Admin Cars hyperLoyalty qısa yazı. en: Short admin Cars hyperLoyalty label. */
    public static String loyaltyLabel(String status) {
        if (OLD.equals(status)) {
            return "köhnə";
        }
        if (REACTIVATED.equals(status)) {
            return "geri qazanılmış";
        }
        if (NEW.equals(status)) {
            return "yeni";
        }
        if (FAILED.equals(status)) {
            return "failed";
        }
        return "—";
    }
}
