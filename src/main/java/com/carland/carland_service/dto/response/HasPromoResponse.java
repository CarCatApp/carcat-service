package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Token cevabı. hasPromo false ise promo null.
 * en: Token response. promo is null when hasPromo is false.
 */
@Data
@Builder
public class HasPromoResponse {
    boolean hasPromo;
    UserPromoResponse promo;
}
