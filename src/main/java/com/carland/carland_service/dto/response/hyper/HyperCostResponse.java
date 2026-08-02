package com.carland.carland_service.dto.response.hyper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * tr: Hyper API'den gelen parasal değeri (tutar ve para birimi) eşleyen yanıt DTO'su; Hyper servis geçmişi yanıtlarında kullanılır.
 * en: Response DTO mapping a monetary value (amount and currency) from the Hyper API; used in Hyper service history responses.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperCostResponse {
    private BigDecimal amount;
    private String currency;
}
