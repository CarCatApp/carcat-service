package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tekil hizmet. Ad offered_services kopyasından, dil başlığına göre.
 * Fiyat aralığının kaynağı yok: priceMin ve priceMax boş döner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingServiceLineView {
    String name;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
}
