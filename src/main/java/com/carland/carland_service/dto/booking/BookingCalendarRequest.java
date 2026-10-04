package com.carland.carland_service.dto.booking;

import lombok.Data;

import java.util.List;

/**
 * tr: Flutter ay takvimi isteği. issue boşsa təmir+yoxlama yok.
 * en: Flutter month-calendar request. A blank issue means no repair+inspection.
 */
@Data
public class BookingCalendarRequest {
    Long packageId;
    List<Long> individualServiceIds;
    String issue;
}
