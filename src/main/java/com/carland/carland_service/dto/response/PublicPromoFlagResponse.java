package com.carland.carland_service.dto.response;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Builder;
import lombok.Value;

/**
 * tr: Giriş öncesi promo kutusu. Rol istemciden gelmez. Sunucu USER satırını okur.
 * en: Pre-login promo field. The client does not send a role. The server reads the USER row.
 */
@Value
@Builder
public class PublicPromoFlagResponse {
    String name;
    FeatureFlagState state;
}
