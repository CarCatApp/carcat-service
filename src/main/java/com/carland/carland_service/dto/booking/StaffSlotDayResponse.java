package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Bir günün staff slotları.
 * en: Staff slots for one day.
 */
@Data
@Builder
public class StaffSlotDayResponse {
    Long branchId;
    String day;
    List<StaffSlotView> slots;
}
