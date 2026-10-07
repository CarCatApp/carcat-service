package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Saati gizle veya geri aç. Satır silinmez.
 * en: Hide or reopen the hour. The row is not deleted.
 */
@Data
public class StaffSlotHiddenRequest {
    Boolean hidden;
}
