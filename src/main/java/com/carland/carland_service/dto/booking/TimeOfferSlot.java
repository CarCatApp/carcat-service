package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeOfferSlot {
    Long rangeId;
    /** HH:mm */
    String start;
    /** HH:mm */
    String end;
}
