package com.carland.carland_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tr: Dahili ekip dokümantasyonu için OpenAPI/Swagger konfigürasyonu.
 * en: OpenAPI / Swagger configuration for internal team documentation.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Value("${carland.swagger.service-server-url:https://digital-innovation.agency/carland/server-carland}")
    private String serviceServerUrl;

    private static final Set<String> KONG_INJECTED_HEADERS = Set.of(
            "X-User-Id", "phoneNumber", "role", "inviterId"
    );

    private static final Map<String, String> KONG_HEADER_BASE_DESC = Map.of(
            "X-User-Id", "User id from JWT claims.",
            "phoneNumber", "Phone number from JWT claims.",
            "role", "Role from JWT claims (USER / ADMIN / SUPER_ADMIN / BOSS).",
            "inviterId", "Inviter id from JWT claims (invite flows)."
    );

    /**
     * tr: Ana OpenAPI tanımını üretir.
     * en: Produces the main OpenAPI definition.
     */
    @Bean
    public OpenAPI carlandServiceOpenAPI() {
        String description = """
                Internal API documentation for **carland_service**.

                ### Login in Swagger
                Use the black **Carland Login** bar (phone + password). It calls **carland_auth** `/login`,
                stores `accessToken` + `refreshToken`, and auto-fills **Authorize → bearerAuth**.
                When the access token expires (or a Try-it-out call returns 401), the page renews it via `/refresh`.

                ### Kong identity headers
                `X-User-Id`, `phoneNumber`, `role`, `inviterId` are injected by Kong Lua after JWT validation.
                In Swagger they stay **required** for documentation fidelity but are **read only** and labelled
                **oto ekleme kong function** — do not type them manually in production.

                ### Defaults
                - `Accept-Language` = `az`
                - `X-Client-Timezone` = `Asia/Baku`

                ### Unified catalogue
                This Swagger UI also lists **Carland Auth** and **Webhook Gateway** (dropdown top-right).
                Their OpenAPI specs are aggregated by this service; Try it out uses the public gateway URLs.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Carland Service API")
                        .description(description)
                        .version("v1")
                        .contact(new Contact().name("Carland Engineering")))
                .servers(List.of(new Server()
                        .url(serviceServerUrl)
                        .description("Production via Kong/nginx")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Filled automatically by Carland Login (or paste JWT without 'Bearer ').")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .tags(List.of(
                        new Tag().name("car-controller").description("Cars, percentages, records, VIN history"),
                        new Tag().name("user-controller").description("User details and notifications"),
                        new Tag().name("photo-controller").description("Photos for cars, users, partners"),
                        new Tag().name("group-by-controller").description("Reference lookups"),
                        new Tag().name("webhook-controller").description("Partner Hyper webhook ingest/update")));
    }

    @Bean
    public GroupedOpenApi mobileApiGroup() {
        return GroupedOpenApi.builder()
                .group("mobile-api")
                .displayName("Mobile API (/api/v1)")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi partnerWebhookGroup() {
        return GroupedOpenApi.builder()
                .group("partner-webhooks")
                .displayName("Partner Webhooks (/webhook)")
                .pathsToMatch("/webhook/**")
                .build();
    }

    @Bean
    public OperationCustomizer headerDocumentationCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) {
                return operation;
            }

            operation.getParameters().removeIf(param ->
                    "header".equalsIgnoreCase(param.getIn())
                            && "Authorization".equalsIgnoreCase(param.getName()));

            for (Parameter param : operation.getParameters()) {
                if (!"header".equalsIgnoreCase(param.getIn()) || param.getName() == null) {
                    continue;
                }
                String name = param.getName();

                if (KONG_INJECTED_HEADERS.contains(name)) {
                    String base = KONG_HEADER_BASE_DESC.getOrDefault(name, name);
                    param.setDescription(base + " — oto ekleme kong function");
                    param.setRequired(true);
                    param.setSchema(new StringSchema().readOnly(true));
                    continue;
                }

                if ("Accept-Language".equalsIgnoreCase(name)) {
                    param.setDescription("Client locale for localized messages.");
                    param.setSchema(new StringSchema()._default("az").example("az"));
                    continue;
                }

                if ("X-Client-Timezone".equalsIgnoreCase(name)) {
                    param.setDescription("Client device timezone.");
                    param.setSchema(new StringSchema()._default("Asia/Baku").example("Asia/Baku"));
                }
            }
            return operation;
        };
    }
}
