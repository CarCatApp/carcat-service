package com.carland.carland_service.service.commission;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: İlk randevudaki Hyper VIN cevabı. Saat yok, yalnız servis günleri.
 * en: Hyper VIN answer at the first booking. No clock, only service dates.
 */
public record HyperVinSnapshot(Kind kind, List<LocalDate> dates, String detail) {

    public enum Kind {
        FOUND,
        NOT_FOUND,
        FAILED
    }

    public static HyperVinSnapshot found(List<LocalDate> dates, String detail) {
        return new HyperVinSnapshot(Kind.FOUND, dates == null ? List.of() : List.copyOf(dates), detail);
    }

    public static HyperVinSnapshot notFound(String detail) {
        return new HyperVinSnapshot(Kind.NOT_FOUND, List.of(), detail);
    }

    public static HyperVinSnapshot failed(String detail) {
        return new HyperVinSnapshot(Kind.FAILED, List.of(), detail);
    }
}
