package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * tr: Şubenin gördüğü fərdi xidmət. active şube durumudur. Fiyatlar manat.
 * en: Individual service as the branch sees it. active is the branch status. Prices are whole manat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffIndividualServiceView {
    Long id;
    String code;
    Map<String, String> title;
    Boolean active;
    Integer priceSimple;
    Integer priceMedium;
    Integer priceComplex;
}
