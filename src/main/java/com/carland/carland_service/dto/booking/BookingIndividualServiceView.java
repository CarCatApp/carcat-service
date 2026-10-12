package com.carland.carland_service.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    /** Kalan ömür yüzdesi. 0 süresi dolmuş, 100 yeni yapılmış. Bilgi yoksa null. */
    Integer individualServiceMappedPercentage;
    /** servicedStatus boşken yüzde yerine gösterilen cümle. */
    String percentageNote;
    /** Kalan km. Percentages ile aynı: nextServiceKm − araç km, 0’ın altına inmez. Araç veya kayıt yoksa null. */
    Integer remainingKm;
    /** Sonraki servise kalan gün. Percentages remainingMonths ile aynı sayı. Araç veya tarih yoksa null. */
    Integer remainingMonth;
    /** CAR_SERVICE eşleşmesinde true. Alan yoksa istemci false okur. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean preselected;
    /** Gönderilen percentages serviceId. Yalnız işaretli satırda durur. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long catalogServiceId;
}
