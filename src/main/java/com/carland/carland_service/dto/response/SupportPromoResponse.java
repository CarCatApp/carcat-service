package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Token'daki müşterinin promo'sunu bu şube kabul ediyor mu.
 * en: Whether this branch accepts the token customer's promo.
 */
@Data
@Builder
public class SupportPromoResponse {
    boolean supportPromo;
}
