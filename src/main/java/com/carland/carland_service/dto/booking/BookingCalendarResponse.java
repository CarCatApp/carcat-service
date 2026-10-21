package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Bugünden şubenin son ayının sonuna her gün: date ve available.
 * en: Each day from today through the end of the branch's last month: date and available.
 */
@Data
@Builder
public class BookingCalendarResponse {
    List<BookingCalendarDayView> days;
}
