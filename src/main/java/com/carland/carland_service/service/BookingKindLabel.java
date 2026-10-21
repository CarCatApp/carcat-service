package com.carland.carland_service.service;

/**
 * tr: Slot satırındaki xidmət tipi. İkisi birden seçilince birleşik ad.
 * en: Service type on a slot row. Both choices use the joined name.
 */
public final class BookingKindLabel {

    public static final String ROUTINE = "Dövri qulluq";
    public static final String REPAIR = "Təmir və diaqnostika";

    private BookingKindLabel() {
    }

    public static String of(boolean routine, boolean repair) {
        if (routine && repair) {
            return ROUTINE + " + " + REPAIR;
        }
        if (repair) {
            return REPAIR;
        }
        if (routine) {
            return ROUTINE;
        }
        return null;
    }
}
