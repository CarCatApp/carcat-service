package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Şube kategori aç/kapa gövdesi. active zorunlu, branchId opsiyonel.
 * en: Branch category on/off body. active is required, branchId is optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCatalogCategoryToggleRequest {
    Long branchId;
    Boolean active;
}
