package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Üç seviye fiyat. Boş alan o seviyeyi temizler. Durumu değiştirmez.
 * en: Three tier prices. A null field clears that tier. Status is not changed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffIndividualServicePricesRequest {
    Integer priceSimple;
    Integer priceMedium;
    Integer priceComplex;
}
