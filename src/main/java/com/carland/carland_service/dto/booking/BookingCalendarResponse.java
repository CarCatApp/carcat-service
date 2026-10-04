package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Bugünden ay sonuna her gün: date ve available.
 * en: Each day from today through month end: date and available.
 */
@Data
@Builder
public class BookingCalendarResponse {
    List<BookingCalendarDayView> days;
}
