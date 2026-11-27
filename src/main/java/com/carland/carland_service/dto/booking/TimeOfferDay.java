package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TimeOfferDay {
    /** yyyy-MM-dd */
    String day;
    /** dd.MM */
    String label;
    List<TimeOfferSlot> slots;
}
