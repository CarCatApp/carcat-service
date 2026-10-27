package com.carland.carland_service.dto.request;

import lombok.Data;

import java.util.List;

/**
 * tr: Paket oluşturma ve redaktə. serviceIds açık olan xidmətlərdir.
 * en: Create and edit body. serviceIds are the services switched on.
 */
@Data
public class StaffCarePackageSaveRequest {
    Long branchId;
    String name;
    /** Optional. Stored and returned as packageInfo. Blank clears it. */
    String packageInfo;
    Integer price;
    Boolean active;
    List<Long> serviceIds;
}
