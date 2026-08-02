package com.carland.carland_service.enums;

import java.util.Optional;

/**
 * tr: Hyper'ın universalServiceId değerlerini veritabanındaki servis adlarına (services.name_en) eşleyen enum.
 * en: Enum mapping Hyper's universalServiceId values to service names in the database (services.name_en).
 */
/**
 * Maps Hyper's raw {@code universalServiceId} to {@code services.name_en}.
 *
 * <p>Sync matches {@code Percentage.serviceNameEn} via {@link #matches(String, String)}.</p>
 *
 * <p>Primary value = exact DB {@code name_en}. {@code extraHyperIds} = alternate Hyper id spellings
 * for the same service row. Unmapped values (e.g. {@code "other"}) are skipped silently.</p>
 */
public enum HyperServiceMapping {

    AIR_FILTER("Air filter"),
    BATTERY("Battery"),
    BRAKE_FLUID("Brake fluid"),
    BRAKE_PADS("Brake pads"),
    CABIN_FILTER("Cabin filter"),
    COOLANT("Coolant (antifreeze)"),
    ENGINE_OIL("Engine oil & filter"),
    FUEL_FILTER("Fuel filter"),
    GAS_FILTER("Gas filter"),
    GAS_INJECTORS("Gas injectors"),
    GLOW_PLUGS("Glow plugs"),
    HV_BATTERY_COOLANT("HV battery / power-electronics coolant"),
    INVERTER_COOLANT("Inverter Coolant (antifreeze)"),
    POWER_STEERING_FLUID("Power steering fluid"),
    REDUCTION_GEAR_OIL("Reduction-gear oil"),
    SPARK_PLUGS("Spark plugs"),
    TIMING_BELT("Timing belt"),
    TRANSMISSION_FLUID("Transmission fluid"),
    /** DB {@code name_en} is {@code Tyres}; Hyper may send {@code Tyres} or {@code Tires}. */
    TYRES("Tyres", "Tires"),
    VAPORISER_SERVICE("Vaporiser service"),
    WHEEL_ALIGNMENT("Wheel alignment"),
    WHEEL_BALANCING("Wheel balancing & rotation"),
    WHEEL_BALANCING_COMPACT("Wheel balancing&rotation", "Wheel balancing & rotation");

    /** Canonical {@code services.name_en} (exact DB value). */
    private final String nameEn;

    /** Alternate Hyper {@code universalServiceId} spellings for this {@code name_en}. */
    private final String[] extraHyperIds;

    HyperServiceMapping(String nameEn, String... extraHyperIds) {
        this.nameEn = nameEn;
        this.extraHyperIds = extraHyperIds;
    }

    public String getNameEn() {
        return nameEn;
    }

    /**
     * True when {@code hyperUniversalServiceId} maps to the given {@code percentageNameEn} row.
     */
    public static boolean matches(String hyperUniversalServiceId, String percentageNameEn) {
        if (hyperUniversalServiceId == null || hyperUniversalServiceId.isBlank()
                || percentageNameEn == null || percentageNameEn.isBlank()) {
            return false;
        }
        String hyperId = hyperUniversalServiceId.trim();
        for (HyperServiceMapping mapping : values()) {
            if (!mapping.nameEn.equalsIgnoreCase(percentageNameEn)) {
                continue;
            }
            if (mapping.nameEn.equalsIgnoreCase(hyperId)) {
                return true;
            }
            for (String id : mapping.extraHyperIds) {
                if (id.equalsIgnoreCase(hyperId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolve a Hyper id to {@code name_en} when the mapping is unambiguous.
     * Returns empty when unknown or when multiple rows could match the same Hyper id.
     */
    public static Optional<String> toNameEn(String hyperUniversalServiceId) {
        if (hyperUniversalServiceId == null || hyperUniversalServiceId.isBlank()) {
            return Optional.empty();
        }
        String hyperId = hyperUniversalServiceId.trim();
        String matched = null;
        for (HyperServiceMapping mapping : values()) {
            if (!mapping.hyperIdMatches(hyperId)) {
                continue;
            }
            if (matched != null && !matched.equalsIgnoreCase(mapping.nameEn)) {
                return Optional.empty();
            }
            matched = mapping.nameEn;
        }
        return Optional.ofNullable(matched);
    }

    private boolean hyperIdMatches(String hyperId) {
        if (nameEn.equalsIgnoreCase(hyperId)) {
            return true;
        }
        for (String id : extraHyperIds) {
            if (id.equalsIgnoreCase(hyperId)) {
                return true;
            }
        }
        return false;
    }
}
