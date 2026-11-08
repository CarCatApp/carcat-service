package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: Hyper VIN cavabı. Saat yoxdur, tarixlər var. raw sorğunun gövdəsidir, token yoxdur.
 * en: Hyper VIN answer. No clock, dates only. raw is the response body, never a token.
 */
public record HyperVinSnapshot(Kind kind, List<LocalDate> dates, String detail, String raw) {

    public enum Kind {
        FOUND,
        NOT_FOUND,
        FAILED
    }

    public static HyperVinSnapshot found(List<LocalDate> dates, String detail) {
        return found(dates, detail, null);
    }

    public static HyperVinSnapshot found(List<LocalDate> dates, String detail, String raw) {
        return new HyperVinSnapshot(Kind.FOUND, dates == null ? List.of() : List.copyOf(dates), detail, raw);
    }

    public static HyperVinSnapshot notFound(String detail) {
        return notFound(detail, null);
    }

    public static HyperVinSnapshot notFound(String detail, String raw) {
        return new HyperVinSnapshot(Kind.NOT_FOUND, List.of(), detail, raw);
    }

    public static HyperVinSnapshot failed(String detail) {
        return failed(detail, null);
    }

    public static HyperVinSnapshot failed(String detail, String raw) {
        return new HyperVinSnapshot(Kind.FAILED, List.of(), detail, raw);
    }
}
