package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * tr: Servis geçmişi kaydında kullanılan tek bir parçayı (ad, miktar, birim, maliyet, indirim) temsil eden yanıt DTO'su.
 * en: Response DTO representing a single part used in a service history entry (name, quantity, unit, cost, discount).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHistoryPartResponse {
    private String name;
    private BigDecimal qty;
    private String unit;
    private BigDecimal cost;
    private BigDecimal finalCost;
    private BigDecimal discount;
}
