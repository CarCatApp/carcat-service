package com.carland.carland_service.dto.response.hyper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * tr: Hyper kimlik doğrulama servisinden dönen token cevabının modelidir; JSON alanları @JsonProperty
 *     ile Java alanlarına eşlenir.
 * en: Represents the token response received from Hyper authentication service; JSON response fields
 *     are mapped to Java fields using @JsonProperty.
 */
@Data
public class HyperTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
