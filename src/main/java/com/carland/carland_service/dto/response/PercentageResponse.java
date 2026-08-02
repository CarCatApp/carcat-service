package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * tr: Bir aracın tüm servis kalemlerinin bakım yüzdesi listesini döndüren üst seviye yanıt DTO'su (percentage akışı).
 * en: Top-level response DTO returning the list of maintenance percentages for all of a car's service items (percentage flow).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PercentageResponse {
    Long carId;
    String vin;
    List<CarServicePercentageResponse> responseList;
}
