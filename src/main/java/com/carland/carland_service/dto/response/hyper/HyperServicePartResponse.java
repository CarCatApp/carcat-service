package com.carland.carland_service.dto.response.hyper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * tr: Hyper API servis geçmişi kaydındaki tek bir parçayı (ad, miktar, birim) eşleyen yanıt DTO'su.
 * en: Response DTO mapping a single part within a Hyper API service history record (name, quantity, unit).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperServicePartResponse {
    private String name;
    private BigDecimal qty;
    private String unit;
}
