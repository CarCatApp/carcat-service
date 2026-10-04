package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Seçilen günün yazılabilən saatləri.
 * en: Bookable hours of the chosen day.
 */
@Data
@Builder
public class BookingDayRangesResponse {
    List<BookingDayRangeView> ranges;
}
