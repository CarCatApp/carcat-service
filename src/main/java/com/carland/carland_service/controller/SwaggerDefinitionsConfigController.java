package com.carland.carland_service.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Swagger UI "Select a definition" listesini sabit 4 maddeye kilitleyen config endpoint'i
 *     (GroupedOpenApi duplicate girişlerini engeller).
 * en: Config endpoint that locks the Swagger UI "Select a definition" list to exactly 4 entries
 *     (prevents duplicate GroupedOpenApi entries).
 */
@RestController
public class SwaggerDefinitionsConfigController {

    public static final String AUTH_DEFINITION = "1. Auth — Login / Register / Tokens";
    public static final String MOBILE_DEFINITION = "2. Carland — Mobile API";
    public static final String RECEIVER_DEFINITION = "3. Carland — Partner Webhook Receiver";
    public static final String GATEWAY_DEFINITION = "4. Webhook Gateway — Partner Edge Adapter";

    @GetMapping(value = "/swagger-definitions-config", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> swaggerDefinitionsConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("urls", List.of(
                url(AUTH_DEFINITION, "/v3/api-docs/external/carland-auth"),
                url(MOBILE_DEFINITION, "/v3/api-docs/mobile-api"),
                url(RECEIVER_DEFINITION, "/v3/api-docs/partner-webhooks"),
                url(GATEWAY_DEFINITION, "/v3/api-docs/external/webhook")
        ));
        config.put("urls.primaryName", AUTH_DEFINITION);
        config.put("validatorUrl", "");
        config.put("persistAuthorization", true);
        config.put("tryItOutEnabled", true);
        config.put("displayRequestDuration", true);
        return config;
    }

    private static Map<String, String> url(String name, String path) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("url", path);
        return entry;
    }
}
