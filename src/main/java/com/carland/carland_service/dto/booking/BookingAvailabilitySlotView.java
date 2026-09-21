package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAvailabilitySlotView {
    Long slotId;
    String start;
    String end;
    Integer capacity;
    Integer bookedCount;
    Integer remaining;
    String bookingMode;
    String serviceKey;
    String status;
    Boolean bookable;
}
