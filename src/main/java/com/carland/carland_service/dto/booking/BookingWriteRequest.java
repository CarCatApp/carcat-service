package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWriteRequest {
    Long branchId;
    Long slotId;
    List<String> serviceKeys;
    /** Şubenin branch_care_packages id. Yoksa paket dönmez. */
    Long carePackageId;
    /** Tekil hizmet: offered_services id. Fiyat aralığı yazılmaz. */
    List<Long> offeredServiceIds;
    /** Ignored on create; VIN is copied from the owned car. Quote does not use it. */
    String vin;
    /** Required on create. Must belong to X-User-Id. Quote does not require it. */
    Long carId;
}
