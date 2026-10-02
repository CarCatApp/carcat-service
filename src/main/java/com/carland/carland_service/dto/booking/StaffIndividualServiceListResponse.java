package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Katalogdaki aktiv fərdi xidmətlər ve bu şubenin fiyat/durumu. Kapalı satırlar da gelir.
 * en: Active catalog individual services plus this branch's price and status. Turned-off rows are included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffIndividualServiceListResponse {
    Long branchId;
    List<StaffIndividualServiceView> services;
}
