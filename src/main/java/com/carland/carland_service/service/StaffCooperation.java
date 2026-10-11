package com.carland.carland_service.service;

import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffStatus;

/**
 * A person can log in only when they were not hand-closed and their place is open.
 * Hand-close is {@code manuallyDisabled}. Place-close is partner or branch {@code active}.
 */
public final class StaffCooperation {

    public static final String ALLOWED = "ALLOWED";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String ABSENT = "ABSENT";

    private StaffCooperation() {
    }

    public static boolean placeOpen(BookingStaff row) {
        if (row == null) {
            return false;
        }
        if (Boolean.TRUE.equals(row.getManuallyDisabled())) {
            return false;
        }
        if (BookingStaffStatus.DISABLED.name().equals(row.getStatus())) {
            return false;
        }
        Partner partner = row.getPartner();
        if (partner == null || Boolean.FALSE.equals(partner.getActive())) {
            return false;
        }
        Branch branch = row.getBranch();
        return branch == null || !Boolean.FALSE.equals(branch.getActive());
    }
}
