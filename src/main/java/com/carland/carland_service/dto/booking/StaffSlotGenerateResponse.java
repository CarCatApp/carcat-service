package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Üretim sonucu. message paneldeki tek cümledir.
 * en: Generation result. message is the single sentence shown in the panel.
 */
@Data
@Builder
public class StaffSlotGenerateResponse {
    int created;
    int createdDays;
    List<StaffSlotSkipView> skipped;
    String message;
}
