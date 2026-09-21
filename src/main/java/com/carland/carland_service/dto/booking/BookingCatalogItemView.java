package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCatalogItemView {
    String serviceKey;
    Long branchId;
    String type;
    Map<String, String> title;
    Integer durationMin;
    String durationNote;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
    List<String> includedServiceKeys;
    List<String> brands;
    Boolean active;
}