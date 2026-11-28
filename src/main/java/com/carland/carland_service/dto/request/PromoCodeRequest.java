package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Promo kodu. customerId = customers.user_id.
 * en: Promo code. customerId is customers.user_id.
 */
@Data
public class PromoCodeRequest {
    Long customerId;
    String promoCode;
}
