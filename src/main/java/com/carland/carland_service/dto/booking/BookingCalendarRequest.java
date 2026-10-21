package com.carland.carland_service.dto.booking;

import lombok.Data;

import java.util.List;

/**
 * tr: Flutter takvim isteği. issue boşsa təmir+yoxlama yok.
 * en: Flutter calendar request. A blank issue means no repair+inspection.
 */
@Data
public class BookingCalendarRequest {
    Long packageId;
    List<Long> individualServiceIds;
    String issue;
    /** Bu arabanın pending veya accepted book'u olan saat günü boşaltırsa gün unavailable olur. */
    Long carId;
}
