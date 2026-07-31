package com.carland.carland_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI / Swagger configuration for internal team documentation.
 *
 * <p>This is documentation-only wiring: it does not change any controller, service or security
 * behaviour. It describes the API surface as it exists today, including how identity headers are
 * provided by the Kong JWT plugin rather than by the client.</p>
 *
 * <p>Swagger UI: <code>/swagger-ui.html</code> &nbsp; OpenAPI JSON: <code>/v3/api-docs</code></p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    /** Identity headers injected by the Kong JWT plugin (extracted from the token via Lua). */
    private static final Map<String, String> KONG_INJECTED_HEADERS = Map.of(
            "X-User-Id", "Injected by the Kong JWT plugin from the token claims. Clients do NOT send this manually — Kong adds it after validating the JWT.",
            "phoneNumber", "Injected by the Kong JWT plugin from the token claims. Clients do NOT send this manually.",
            "role", "Injected by the Kong JWT plugin from the token claims (e.g. USER / ADMIN / SUPER_ADMIN). Clients do NOT send this manually.",
            "inviterId", "Injected by the Kong JWT plugin from the token claims for invite flows. Clients do NOT send this manually."
    );

    /** Headers that the client (mobile app) genuinely sends. */
    private static final Map<String, String> CLIENT_HEADERS = Map.of(
            "Accept-Language", "Client locale for localized messages (e.g. az / en / ru).",
            "X-Client-Timezone", "Client device timezone, sent by the mobile app (e.g. Asia/Baku)."
    );

    @Bean
    public OpenAPI carlandServiceOpenAPI() {

        String description = """
                Internal API documentation for **carland_service** — the core backend for cars, customers,
                maintenance templates, service-life percentages, partner service visits (Hyper integration),
                photos and notifications.

                ### Authentication & identity flow
                Clients authenticate by sending `Authorization: Bearer <JWT>`. In production the request first
                reaches the **Kong** gateway, which validates the JWT and, via its JWT plugin (Lua), injects the
                caller's identity into headers (`X-User-Id`, `phoneNumber`, `role`, `inviterId`) before forwarding
                to this service. The service itself reads those injected headers — clients never set them by hand.
                Use the **Authorize** button to attach the bearer token.

                ### Endpoint groups
                - **Mobile API** (`/api/v1/**`) — the JSON API consumed by the mobile app through Kong.
                - **Partner Webhooks** (`/webhook/partner/**`) — partner-facing (Hyper) endpoints protected by an
                  `X-Internal-Token` header and a per-partner `X-Signature` HMAC. These are documented for reference;
                  they cannot be exercised from "Try it out" because they require a valid signature.

                The `/admin/**` (Thymeleaf HTML pages) and `/legal/**` surfaces are intentionally not included here,
                as they are not JSON APIs.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Carland Service API")
                        .description(description)
                        .version("v1")
                        .contact(new Contact().name("Carland Engineering")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT here (without the 'Bearer ' prefix). "
                                        + "Kong validates it and injects the identity headers.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .tags(List.of(
                        new Tag().name("car-controller").description("Cars, service-life percentages, service records and VIN history"),
                        new Tag().name("user-controller").description("User details, notifications and customer cars"),
                        new Tag().name("photo-controller").description("Upload / fetch / delete photos for cars, users and partners"),
                        new Tag().name("group-by-controller").description("Reference lookups: brands, models, body/engine/transmission types, years"),
                        new Tag().name("webhook-controller").description("Partner (Hyper) service-visit ingest and update — HMAC-signed")));
    }

    /** Mobile app JSON API. */
    @Bean
    public GroupedOpenApi mobileApiGroup() {
        return GroupedOpenApi.builder()
                .group("mobile-api")
                .displayName("Mobile API (/api/v1)")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    /** Partner-facing webhook endpoints (documentation reference only). */
    @Bean
    public GroupedOpenApi partnerWebhookGroup() {
        return GroupedOpenApi.builder()
                .group("partner-webhooks")
                .displayName("Partner Webhooks (/webhook)")
                .pathsToMatch("/webhook/**")
                .build();
    }

    /**
     * Enriches operation headers with descriptions so a new team member understands which headers the
     * client sends versus which ones Kong injects. Also drops the explicit {@code Authorization} header
     * parameter, since it is represented by the global bearer security scheme (the Authorize button).
     */
    @Bean
    public OperationCustomizer headerDocumentationCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) {
                return operation;
            }

            operation.getParameters().removeIf(param ->
                    "header".equalsIgnoreCase(param.getIn()) && "Authorization".equalsIgnoreCase(param.getName()));

            for (Parameter param : operation.getParameters()) {
                if (!"header".equalsIgnoreCase(param.getIn()) || param.getName() == null) {
                    continue;
                }
                String kongDesc = KONG_INJECTED_HEADERS.get(param.getName());
                if (kongDesc != null) {
                    param.setDescription(kongDesc);
                    continue;
                }
                String clientDesc = CLIENT_HEADERS.get(param.getName());
                if (clientDesc != null) {
                    param.setDescription(clientDesc);
                }
            }
            return operation;
        };
    }
}
