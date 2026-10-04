package com.carland.carland_service.dto.request;

import lombok.Data;

import java.time.LocalTime;

/**
 * tr: Bir gün tipinin saat, yer sayısı ve ani/təsdiqli kuralı. Ara boşsa öğle yok.
 * en: Hours, places, and instant/approval for one kind of day. A null break means no lunch.
 */
@Data
public class StaffSlotDayRule {
    LocalTime start;
    LocalTime end;
    Integer capacity;
    String bookingMode;
    LocalTime breakStart;
    LocalTime breakEnd;
}
