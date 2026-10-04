package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Ayın bir günü. date gün.ay.yıl. available yazılabilen saat varsa true.
 * en: One day. date is day.month.year. available is true when a bookable hour exists.
 */
@Data
@Builder
public class BookingCalendarDayView {
    String date;
    Boolean available;
}
