package com.carland.carland_service.enums;

/**
 * tr: Percentage kaydının yaşam döngüsü durumunu (oluşturuldu, müşteri düzenledi, partner düzenledi) tanımlayan enum.
 * en: Enum defining the lifecycle state of a Percentage row (created, edited by customer, edited by partner).
 */
/**
 * Lifecycle state of a {@code Percentage} row.
 * Stored as a string in the {@code percentages.status} column.
 *
 * <p>This is a completely separate concept from {@code servicedStatus}
 * (serviced / never_serviced / '') and must not be mixed with it.</p>
 */
public enum PercentageStatus {

    /**
     * Backend created / calculated the value, nobody overrode it.
     */
    CREATED,

    /**
     * Customer overrode the value via editPercentage.
     */
    EDITED_BY_CUSTOMER,

    /**
     * HyperService (partner) data overrode the value. Last service date/km stay
     * locked for the customer; next service date/km remain editable.
     */
    EDITED_BY_PARTNER,

    /**
     * Hidden and inert: not listed, no push, no recalculation side effects.
     * Used for Kia drive/transmission variants and timing-belt=chain.
     */
    DEACTIVATED;

    /**
     * Legacy stored value, kept for backward compatibility with old rows.
     */
    private static final String LEGACY_EDITED = "EDITED";

    /**
     * Null-safe parse of the stored status string.
     * Old rows that have {@code null}/empty are treated as {@link #CREATED};
     * the legacy {@code "EDITED"} value maps to {@link #EDITED_BY_CUSTOMER}.
     */
    public static PercentageStatus fromStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return CREATED;
        }
        String value = raw.trim();
        if (LEGACY_EDITED.equalsIgnoreCase(value)) {
            return EDITED_BY_CUSTOMER;
        }
        try {
            return PercentageStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CREATED;
        }
    }

    /**
     * Customer may always open the edit form. Partner-sourced last service
     * fields are still rejected in {@code editPercentage}.
     */
    public boolean isEditable() {
        return this != DEACTIVATED;
    }

    /**
     * True when the value was set explicitly (customer or partner) rather than auto-calculated.
     */
    public boolean isManuallySet() {
        return this == EDITED_BY_CUSTOMER || this == EDITED_BY_PARTNER;
    }

    /**
     * Listed in the app and eligible for push / execute when true.
     */
    public boolean isActiveForUser() {
        return this != DEACTIVATED;
    }
}
