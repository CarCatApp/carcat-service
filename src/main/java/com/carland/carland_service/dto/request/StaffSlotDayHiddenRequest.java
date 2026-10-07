package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Seçili hizmetin o gününü gizle veya aç. Diğer hizmetler durur.
 * en: Hide or reopen the selected service on that day. Other services stay.
 */
@Data
public class StaffSlotDayHiddenRequest {
    Long branchId;
    String day;
    Long packageId;
    Long individualServiceId;
    Boolean repairInspection;
    Boolean hidden;
}
