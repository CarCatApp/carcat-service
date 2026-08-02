package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * tr: Parasal bir değeri (tutar ve para birimi) taşıyan yardımcı yanıt DTO'su; masraf/harcama yanıtlarında kullanılır.
 * en: Helper response DTO carrying a monetary value (amount and currency); used in cost/spending responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyResponse {
    private BigDecimal amount;
    private String currency;
}
