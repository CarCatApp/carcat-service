package com.carland.carland_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * tr: Veritabanındaki partners.id kayıtlarıyla eşleşen sabit iş ortağı kimliklerini (Hyper, AvtoVaz) tanımlayan enum.
 * en: Enum defining stable partner ids (Hyper, AvtoVaz) matching the partners.id rows in the database.
 */
/**
 * Stable partner registry ids — must match {@code partners.id} rows seeded in DB.
 * {@code Visit.serviceCenterId} and v1 {@code ServiceHistory.serviceCenterId} store these values.
 */
@Getter
@RequiredArgsConstructor
public enum PartnerId {

    HYPER(1L, "hyper", "HyperService"),
    AVTOVAZ(2L, "avtovaz", "AvtoVaz");

    private final Long id;
    private final String source;
    private final String defaultName;

    public static Optional<PartnerId> fromId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(partner -> partner.id.equals(id))
                .findFirst();
    }

    public static Optional<PartnerId> fromSource(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(partner -> partner.source.equalsIgnoreCase(source.trim()))
                .findFirst();
    }
}
