package com.carland.carland_service.enums;

import java.util.Optional;

/**
 * Maps Hyper's {@code universalServiceId} to DB {@code services.name_en}.
 *
 * <p>Primary value = exact DB {@code name_en}. Extra ids = Hyper codes / aliases
 * (legacy human-readable ids still match via {@code nameEn}; new contract uses ENUM_STYLE codes).</p>
 *
 * <p>Unmapped values (e.g. {@code "other"}) are skipped silently.</p>
 */
public enum HyperServiceMapping {

    AIR_FILTER("Air filter", "AIR_FILTER"),
    BATTERY("Battery", "BATTERY"),
    BRAKE_FLUID("Brake fluid", "BRAKE_FLUID"),
    BRAKE_PADS("Brake pads", "BRAKE_PADS"),
    CABIN_FILTER("Cabin filter", "CABIN_FILTER"),
    COOLANT("Coolant (antifreeze)", "COOLANT"),
    ENGINE_OIL("Engine oil & filter", "ENGINE_OIL"),
    FUEL_FILTER("Fuel filter", "FUEL_FILTER"),
    GAS_FILTER("Gas filter", "GAS_FILTER"),
    GAS_INJECTORS("Gas injectors", "GAS_INJECTORS"),
    GLOW_PLUGS("Glow plugs", "GLOW_PLUGS"),
    HV_BATTERY_COOLANT("HV battery / power-electronics coolant", "HV_BATTERY_COOLANT"),
    INVERTER_COOLANT("Inverter Coolant (antifreeze)", "INVERTER_COOLANT"),
    POWER_STEERING_FLUID("Power steering fluid", "POWER_STEERING_FLUID"),
    REDUCTION_GEAR_OIL("Reduction-gear oil", "REDUCTION_GEAR_OIL"),
    SPARK_PLUGS("Spark plugs", "SPARK_PLUGS"),
    TIMING_BELT("Timing belt", "TIMING_BELT"),
    TRANSMISSION_FLUID("Transmission fluid", "TRANSMISSION_FLUID"),
    /** Legacy Hyper may send Tyres/Tires; new contract TYRES/TIRES. */
    TYRES("Tyres", "Tires", "TYRES", "TIRES"),
    VAPORISER_SERVICE("Vaporiser service", "VAPORISER_SERVICE"),
    WHEEL_ALIGNMENT("Wheel alignment", "WHEEL_ALIGNMENT"),
    WHEEL_BALANCING("Wheel balancing & rotation", "WHEEL_BALANCING"),
    WHEEL_BALANCING_COMPACT("Wheel balancing&rotation", "Wheel balancing & rotation", "WHEEL_BALANCING_COMPACT");

    /** Canonical {@code services.name_en} (exact DB value). */
    private final String nameEn;

    /** Alternate Hyper {@code universalServiceId} values (ENUM_STYLE codes + legacy aliases). */
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
