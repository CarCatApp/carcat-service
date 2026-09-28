package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Şubenin paket listesi. Pasif paketler de gelir.
 * en: Packages for one branch. Inactive packages are included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCarePackageListResponse {
    Long branchId;
    List<StaffCarePackageView> packages;
}
