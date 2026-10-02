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
    Integer priceMax;
    String currency;
    String unit;
}
