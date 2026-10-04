package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Panel satırı. packageId veya individualServiceId veya təmir+yoxlama.
 * en: Panel row. packageId, individualServiceId, or repair+inspection.
 */
@Data
@Builder
public class StaffSlotView {
    Long slotId;
    String day;
    String start;
    String end;
    Integer capacity;
    Integer bookedCount;
    String bookingMode;
    String status;
    Long packageId;
    Long individualServiceId;
    Boolean repairInspection;
}
