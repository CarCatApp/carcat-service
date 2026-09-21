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
public class BookingQuoteResponse {
    Long branchId;
    Long slotId;
    List<String> serviceKeys;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
}
