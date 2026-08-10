package com.carland.carland_service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Auth ve webhook OpenAPI JSON'larını iç ağdan çekip Swagger UI'ye sunar; Try it out için public server URL yazar.
 * en: Fetches auth and webhook OpenAPI JSON from the internal network and serves them to Swagger UI,
 *     rewriting the servers entry to the public gateway URLs for Try it out.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OpenApiAggregationController {

    private final ObjectMapper objectMapper;

    @Value("${carland.swagger.auth-docs-url:http://carland-auth:9090/v3/api-docs/legacy-users}")
    private String authDocsUrl;

    @Value("${carland.swagger.auth-new-docs-url:http://carland-auth:9090/v3/api-docs/new-users}")
    private String authNewDocsUrl;

    @Value("${carland.swagger.webhook-docs-url:http://webhook:8080/v3/api-docs}")
    private String webhookDocsUrl;

    @Value("${carland.swagger.auth-server-url:https://digital-innovation.agency/auth/server}")
    private String authServerUrl;

    @Value("${carland.swagger.webhook-server-url:https://digital-innovation.agency}")
    private String webhookServerUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping(value = "/v3/api-docs/external/carland-auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> carlandAuthDocs() {
        return fetchAndSetServer(authDocsUrl, authServerUrl, "Carland Auth legacy (public gateway)");
    }

    @GetMapping(value = "/v3/api-docs/external/carland-auth-new", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> carlandAuthNewDocs() {
        return fetchAndSetServer(authNewDocsUrl, authServerUrl, "Carland Auth NewUsers (public gateway)");
    }

    @GetMapping(value = "/v3/api-docs/external/webhook", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> webhookDocs() {
        return fetchAndSetServer(webhookDocsUrl, webhookServerUrl, "Webhook gateway (public)");
    }

    private Map<String, Object> fetchAndSetServer(String docsUrl, String serverUrl, String serverDescription) {
        try {
            String body = restClient.get()
                    .uri(docsUrl)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> doc = objectMapper.readValue(body, new TypeReference<>() {});
            Map<String, Object> server = new LinkedHashMap<>();
            server.put("url", serverUrl);
            server.put("description", serverDescription);
            doc.put("servers", List.of(server));
            // Keep upstream info.title/description from auth / webhook OpenAPI configs.
            return doc;
        } catch (Exception ex) {
            log.error("Failed to aggregate OpenAPI from {}", docsUrl, ex);
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("openapi", "3.0.1");
            fallback.put("info", Map.of(
                    "title", "Unavailable: " + docsUrl,
                    "description", "Could not fetch OpenAPI from " + docsUrl + " — " + ex.getMessage()
                            + ". Check Docker service name / SWAGGER_*_DOCS_URL and that springdoc is enabled on that service.",
                    "version", "n/a"
            ));
            fallback.put("paths", Map.of());
            fallback.put("servers", List.of(Map.of("url", serverUrl, "description", serverDescription)));
            return fallback;
        }
    }
}
