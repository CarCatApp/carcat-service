package com.carland.carland_service.service;

import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.CalendarStatus;
import com.carland.carland_service.enums.RangeStatus;

/**
 * tr: Flutter'ın görmemesi gereken saat. Kayıt durur.
 * en: An hour Flutter must not see. The row stays.
 */
public final class SlotOffer {

    private SlotOffer() {
    }

    public static boolean hidden(Range range) {
        if (range == null) {
            return true;
        }
        if (Boolean.TRUE.equals(range.getDayHidden())) {
            return true;
        }
        if (RangeStatus.HIDDEN.name().equals(range.getStatus())) {
            return true;
        }
        Calendar calendar = range.getCalendar();
        return calendar != null && CalendarStatus.HIDDEN.name().equals(calendar.getStatus());
    }
}
