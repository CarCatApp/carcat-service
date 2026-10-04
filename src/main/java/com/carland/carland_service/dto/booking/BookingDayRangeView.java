package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Bir saat penceresi. start və end Bakı saatidir.
 * en: One time window. start and end are Baku clock times.
 */
@Data
@Builder
public class BookingDayRangeView {
    Long rangeId;
    String start;
    String end;
    Integer remaining;
}
