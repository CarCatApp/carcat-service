package com.carland.carland_service.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * tr: Swagger definition listesini yaml ile aynı 4 maddeye zorlar (startup'ta, crash etmeden).
 * en: Forces the Swagger definition list to the same 4 entries as yaml (at startup, safely).
 */
@Component
public class SwaggerUiUrlsConfig implements ApplicationRunner {

    static final String AUTH = "1. Auth — Login / Register / Tokens";
    static final String MOBILE = "2. Carland — Mobile API";
    static final String RECEIVER = "3. Carland — Partner Webhook Receiver";
    static final String GATEWAY = "4. Webhook Gateway — Partner Edge Adapter";

    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    public SwaggerUiUrlsConfig(SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<SwaggerUrl> urls = new LinkedHashSet<>();
        urls.add(new SwaggerUrl(AUTH, "/v3/api-docs/external/carland-auth", AUTH));
        urls.add(new SwaggerUrl(MOBILE, "/v3/api-docs/mobile-api", MOBILE));
        urls.add(new SwaggerUrl(RECEIVER, "/v3/api-docs/partner-webhooks", RECEIVER));
        urls.add(new SwaggerUrl(GATEWAY, "/v3/api-docs/external/webhook", GATEWAY));
        swaggerUiConfigProperties.setUrls(urls);
        swaggerUiConfigProperties.setUrlsPrimaryName(AUTH);
    }
}
