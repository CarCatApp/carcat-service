package com.carland.carland_service.enums;

/**
 * tr: Booking staff JWT / üyelik rolleri. Feature-flag grid'ine eklenmez.
 * en: Booking staff JWT / membership roles. Not added to the feature-flag grid.
 */
public enum BookingStaffRole {
    PARTNER_ADMIN,
    BRANCH_ADMIN,
    /** Whole company. Sees every active branch on the partner dashboard. */
    HEAD_ADMIN
}
