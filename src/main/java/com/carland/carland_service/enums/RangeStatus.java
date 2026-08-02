package com.carland.carland_service.enums;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.Getter;

/**
 * tr: Takvimdeki randevu zaman aralığının durumlarını (müsait, kabul edildi, mola, dolu vb.) tanımlayan enum.
 * en: Enum defining the statuses of an appointment time slot in the calendar (available, accepted, break, full, etc.).
 */
@Getter
public enum RangeStatus {
    AVAILABLE,
    ACCEPTED,
    REJECTED,
    PENDING,
    BREAK,
    PENDING_LOCAL,
    FULL
}
