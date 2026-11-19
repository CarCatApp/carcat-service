package com.carland.carland_service.service.technicalprofile;

/**
 * tr: Excel / katalog {@code name_en} sabitleri. Eşleştirme bu metinlere göre yapılır.
 * en: Excel / catalog {@code name_en} constants. Matching uses these strings.
 */
public final class MaintenanceItemNames {

    private MaintenanceItemNames() {
    }

    /** Non-Kia and Kia gateway until variant is chosen. */
    public static final String TRANSMISSION_FLUID = "Transmission fluid";

    public static final String TRANSMISSION_AT4 =
            "Automatic transmission fluid & filter (AT4)";
    public static final String TRANSMISSION_AT6_8 =
            "Automatic transmission fluid & filter (6AT/8AT)";
    public static final String TRANSMISSION_CVT_DCT =
            "Automatic transmission fluid & filter (CVT/DCT)";
    public static final String TRANSMISSION_MANUAL = "Manual transmission oil";

    public static final String TRANSFER_CASE_OIL = "Transfer case oil (4WD)";
    public static final String DIFFERENTIAL_OIL = "Differential oil";

    public static final String TIMING_BELT = "Timing belt (kit)";
    /** Legacy / short catalog name if present. */
    public static final String TIMING_BELT_SHORT = "Timing belt";

    public static boolean isTimingBelt(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) {
            return false;
        }
        String n = nameEn.trim();
        return TIMING_BELT.equalsIgnoreCase(n)
                || TIMING_BELT_SHORT.equalsIgnoreCase(n)
                || n.toLowerCase().startsWith("timing belt");
    }

    public static boolean isTransmissionGateway(String nameEn) {
        return nameEn != null && TRANSMISSION_FLUID.equalsIgnoreCase(nameEn.trim());
    }

    public static boolean isTransmissionVariant(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) {
            return false;
        }
        String n = nameEn.trim();
        return TRANSMISSION_AT4.equalsIgnoreCase(n)
                || TRANSMISSION_AT6_8.equalsIgnoreCase(n)
                || TRANSMISSION_CVT_DCT.equalsIgnoreCase(n)
                || TRANSMISSION_MANUAL.equalsIgnoreCase(n);
    }

    public static boolean isTransferCase(String nameEn) {
        return nameEn != null && TRANSFER_CASE_OIL.equalsIgnoreCase(nameEn.trim());
    }

    public static boolean isDifferential(String nameEn) {
        return nameEn != null && DIFFERENTIAL_OIL.equalsIgnoreCase(nameEn.trim());
    }
}
