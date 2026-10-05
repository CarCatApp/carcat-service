package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Bugünden itibaren saati olan günler.
 * en: Days from today that have at least one slot.
 */
@Data
@Builder
public class StaffSlotCoverageResponse {
    Long branchId;
    List<StaffSlotCoverageDayView> days;
}
