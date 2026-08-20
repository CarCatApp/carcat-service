package com.carland.carland_service.test_sima_idda.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SIMA / IDDA credentials from env (/root/carland.env). Never hardcode secrets here.
 */
@Getter
@Component
public class SimaIddaProperties {

    @Value("${sima.base-url}")
    private String simaBaseUrl;

    @Value("${sima.identifier}")
    private String simaIdentifier;

    @Value("${sima.hmac-secret}")
    private String simaHmacSecret;

    @Value("${sima.auth-scheme:HMACSHA256}")
    private String simaAuthScheme;

    @Value("${sima.device-info:carland-sima-test}")
    private String simaDeviceInfo;

    @Value("${idda.base-url:https://example-idda-staging.local}")
    private String iddaBaseUrl;

    @Value("${idda.partner-code:}")
    private String iddaPartnerCode;

    @Value("${idda.api-key:}")
    private String iddaApiKey;
}
