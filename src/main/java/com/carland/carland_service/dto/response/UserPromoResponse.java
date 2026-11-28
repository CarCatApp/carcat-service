package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * tr: Müşterinin kabul ettiği promo. Şube listesi yok.
 * en: The promo the customer accepted. No branch list.
 */
@Data
@Builder
public class UserPromoResponse {
    Long id;
    Long bannerId;
    String code;
    String name;
    String description;
    String discountType;
    BigDecimal discountValue;
    String status;
    Boolean expired;
    LocalDate startDate;
    LocalDate endDate;
}
