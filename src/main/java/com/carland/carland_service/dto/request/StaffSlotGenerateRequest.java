package com.carland.carland_service.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * tr: Staff slot üretimi. Paket ve tekil hizmet id ile gider.
 * en: Staff slot generation. Packages and individual services are ids.
 */
@Data
public class StaffSlotGenerateRequest {
    Long branchId;
    List<Long> packageIds;
    List<Long> individualServiceIds;
    Boolean repairInspection;
    LocalDate startDate;
    LocalDate endDate;
    Integer durationMin;
    Boolean saturday;
    Boolean sunday;
    StaffSlotDayRule weekday;
    StaffSlotDayRule weekend;
}
