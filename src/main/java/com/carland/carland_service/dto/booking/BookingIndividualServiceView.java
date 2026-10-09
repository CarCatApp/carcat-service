package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIndividualServiceView {
    Long id;
    String code;
    String name;
    Integer priceMin;
    /** Nisbətən mürəkkəb (price_medium), qəpik. Səviyyə boşdursa null. */
    Integer priceMid;
    Integer priceMax;
    String currency;
    String unit;
    /** Kalan ömür yüzdesi. 0 süresi dolmuş, 100 yeni yapılmış. Kayıt yoksa null. */
    Integer individualServiceMappedPercentage;
}
