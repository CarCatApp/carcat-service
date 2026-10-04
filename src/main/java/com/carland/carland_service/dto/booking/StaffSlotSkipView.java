package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Bu hedef için o gün zaten saat vardı, yazılmadı.
 * en: That day already had hours for this target, so it was not written.
 */
@Data
@Builder
public class StaffSlotSkipView {
    String day;
    String name;
    Long packageId;
    Long individualServiceId;
    Boolean repairInspection;
}
