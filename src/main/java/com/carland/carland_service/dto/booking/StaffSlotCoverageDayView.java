package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Bugünden sonraki bir takvim günü. Boş aradaki günler yok.
 * en: One calendar day from today on. Empty days in between are absent.
 */
@Data
@Builder
public class StaffSlotCoverageDayView {
    String day;
    Long packageId;
    Long individualServiceId;
    Boolean repairInspection;
}
