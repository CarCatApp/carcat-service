package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Staff katalog listesi. branchId kullanılan şubedir.
 * en: Staff catalog list. branchId is the branch that was resolved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCatalogCategoryListResponse {
    Long branchId;
    List<StaffCatalogCategoryView> items;
}
