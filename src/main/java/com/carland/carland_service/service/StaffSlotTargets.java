package com.carland.carland_service.service;

/**
 * tr: Staff slotunun bağlandığı hedef. Paket ve tekil hizmet sayısal id ile durur.
 * en: What a staff slot is tied to. Packages and individual services use numeric ids.
 */
public final class StaffSlotTargets {

    public static final String PACKAGE = "package";
    public static final String INDIVIDUAL = "individual";
    public static final String REPAIR_INSPECTION = "repair_inspection";
    public static final String CALENDAR_CATEGORY = "booking";

    private StaffSlotTargets() {
    }
}
