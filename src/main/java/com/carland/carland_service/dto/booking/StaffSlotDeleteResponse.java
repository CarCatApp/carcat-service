package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Toplu slot silme sonucu. message paneldeki tek cümledir.
 * en: Bulk slot delete result. message is the single sentence shown in the panel.
 */
@Data
@Builder
public class StaffSlotDeleteResponse {
    String message;
}
