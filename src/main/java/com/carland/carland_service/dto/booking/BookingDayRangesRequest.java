package com.carland.carland_service.dto.booking;

import lombok.Data;

import java.util.List;

/**
 * tr: Seçilen günün saatleri. date gün.ay.yıl. issue boşsa təmir yox.
 * en: Hours of the chosen day. date is day.month.year. A blank issue means no repair.
 */
@Data
public class BookingDayRangesRequest {
    String date;
    Long packageId;
    List<Long> individualServiceIds;
    String issue;
}
