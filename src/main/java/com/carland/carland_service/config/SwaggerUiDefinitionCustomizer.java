package com.carland.carland_service.config;

import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * tr: Swagger UI "Select a definition" listesini tam 4 maddeye kilitler
 *     (GroupedOpenApi + yaml urls birleşince oluşan duplicate'leri temizler).
 * en: Locks the Swagger UI definition dropdown to exactly 4 entries
 *     (removes duplicates from merging GroupedOpenApi with yaml urls).
 */
@Component
public class SwaggerUiDefinitionCustomizer {

    public static final String AUTH = "1. Auth — Login / Register / Tokens";
    public static final String MOBILE = "2. Carland — Mobile API";
    public static final String RECEIVER = "3. Carland — Partner Webhook Receiver";
    public static final String GATEWAY = "4. Webhook Gateway — Partner Edge Adapter";

    private final SwaggerUiConfigParameters swaggerUiConfigParameters;

    public SwaggerUiDefinitionCustomizer(SwaggerUiConfigParameters swaggerUiConfigParameters) {
        this.swaggerUiConfigParameters = swaggerUiConfigParameters;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void lockDefinitions() {
        Set<SwaggerUrl> urls = new LinkedHashSet<>();
        // SwaggerUrl(name/group, url, displayName)
        urls.add(new SwaggerUrl(AUTH, "/v3/api-docs/external/carland-auth", AUTH));
        urls.add(new SwaggerUrl(MOBILE, "/v3/api-docs/mobile-api", MOBILE));
        urls.add(new SwaggerUrl(RECEIVER, "/v3/api-docs/partner-webhooks", RECEIVER));
        urls.add(new SwaggerUrl(GATEWAY, "/v3/api-docs/external/webhook", GATEWAY));
        swaggerUiConfigParameters.setUrls(urls);
        swaggerUiConfigParameters.setUrlsPrimaryName(AUTH);
    }
}
