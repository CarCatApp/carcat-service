package com.carland.carland_service.config;

import com.carland.carland_service.controller.FeatureFlagAdminRestController;
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

    @Value("${carland.swagger.admin-server-url:https://digital-innovation.agency}")
    private String adminServerUrl;

    private static final Set<String> KONG_INJECTED_HEADERS = Set.of(
            "X-User-Id", "phoneNumber", "role", "inviterId"
    );

    private static final Map<String, String> KONG_HEADER_BASE_DESC = Map.of(
            "X-User-Id",
            "Extracted from the access JWT by Kong Lua on the server and injected into the request header automatically. Do not set manually.",
            "phoneNumber",
            "Extracted from the access JWT by Kong Lua on the server and injected into the request header automatically. Do not set manually.",
            "role",
            "Extracted from the access JWT by Kong Lua on the server and injected into the request header automatically. Do not set manually.",
            "inviterId",
            "Extracted from the access JWT by Kong Lua on the server and injected into the request header automatically (invite flows). Do not set manually."
    );

    @Bean
    public OpenAPI carlandServiceOpenAPI() {
        String description = """
                Internal API documentation for **carland-service** (core domain microservice).

                ### How to use this Swagger UI
                1. Open **1A. Auth — Legacy** (or **1B. Auth — NewUsers**) — **login or register first**.
                2. Use the black **Carland Login** bar (phone + password). It stores `accessToken` + `refreshToken`
                   and auto-fills **Authorize → bearerAuth** for all JWT-protected Try-it-out calls.
                3. Access JWT TTL ≈ **15 minutes**. On **401** (or near expiry) the UI renews via `/refresh`.
                4. Switch definitions as needed — the Bearer token stays in the browser session.

                ### Select a definition (catalogue)
                | Definition | Microservice | Role |
                |---|---|---|
                | **1A. Auth — Legacy** | **auth-service** | Current app: `/api/v1/users`, `/api/v1/otp` |
                | **1B. Auth — NewUsers** | **auth-service** | Parallel PO flow: `/api/v1/newUsers` |
                | **2. Carland — Mobile API** | **carland-service** | Mobile / app APIs (`/api/v1/**`) |
                | **5. Carland — Admin Feature Flags** | **carland-service** | Panel REST (`/admin/feature-flags`, `/admin/endpoints`). **Panel ADMIN JWT only** |
                | **3. Carland — Partner Webhook Receiver** | **carland-service** | Internal **receiver** for partner visit ingest/update (`/webhook/**`) |
                | **4. Webhook Gateway — Partner Edge Adapter** | **webhook-service** | Public **edge adapter**: partners call here; forwards to carland; queues in RabbitMQ if carland is down |

                ### Partner webhook vs gateway (do not confuse)
                - **Definition 4 (Webhook Gateway)** = adapter facing partner body shops / service centers.
                - **Definition 3 (Carland Receiver)** = same domain operations accepted **inside** carland after the gateway forwards them.
                - They look similar on purpose: gateway adapts & relays; carland persists & applies business rules.

                ### Kong identity headers
                `X-User-Id`, `phoneNumber`, `role`, `inviterId` are **read-only** in Swagger. In production Kong Lua
                extracts them from the access JWT and injects them — do not type them manually.

                ### Defaults
                - `Accept-Language` = `az` (read-only in Swagger)
                - `X-Client-Timezone` = `Asia/Baku` (read-only in Swagger)
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
                        new Tag().name("feature-flags-me").description("Mobile `/me` — caller JWT role, not panel admin"),
                        new Tag().name("admin-feature-flags").description(
                                "Panel feature-flag REST. **Required: panel ADMIN JWT.** Other JWTs → 403."),
                        new Tag().name("photo-controller").description("Photos for cars, users, partners"),
                        new Tag().name("group-by-controller").description("Reference lookups"),
                        new Tag().name("webhook-controller").description(
                                "Partner visit ingest/update **receiver** (called by webhook-service, not by partners directly)")));
    }

    @Bean
    public GroupedOpenApi mobileApiGroup() {
        return GroupedOpenApi.builder()
                .group("mobile-api")
                .displayName("2. Carland — Mobile API")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Carland — Mobile API")
                        .description("""
                                **carland-service** mobile / app APIs (`/api/v1/**`).

                                Requires access JWT from **1. Auth**. Use the **Carland Login** bar first.
                                Kong injects `X-User-Id` / `phoneNumber` from the JWT (read-only fields in Swagger).
                                """))
                .build();
    }

    @Bean
    public GroupedOpenApi adminFeatureFlagsGroup() {
        String adminNote = """
                **Required role: panel ADMIN only.**

                - JWT claim `role=ADMIN` for the designated panel account
                - `Authorization: Bearer <accessToken>` or cookie `ADMIN_ACCESS`
                - Any other user / role → **403 Admin not found**
                - Missing/invalid token → **401 JWT token required**

                Try it out uses the **admin host** (`digital-innovation.agency`), not `/carland/server-carland`.
                Log in with the panel admin account first (not a regular app user).
                """;
        return GroupedOpenApi.builder()
                .group("admin-feature-flags")
                .displayName("5. Carland — Admin Feature Flags")
                .addOpenApiMethodFilter(method ->
                        method.getBeanType().equals(FeatureFlagAdminRestController.class))
                .addOperationCustomizer((operation, handlerMethod) -> {
                    String existing = operation.getDescription();
                    operation.setDescription(existing == null || existing.isBlank()
                            ? adminNote
                            : existing + "\n\n" + adminNote);
                    if (operation.getParameters() != null) {
                        operation.getParameters().removeIf(param ->
                                param.getName() != null
                                        && (param.getName().equalsIgnoreCase("request")
                                        || param.getName().equalsIgnoreCase("httpServletRequest")));
                    }
                    return operation;
                })
                .addOpenApiCustomizer(openApi -> {
                    openApi.setServers(List.of(new Server()
                            .url(adminServerUrl)
                            .description("Admin panel host (not the mobile /carland/server-carland prefix)")));
                    openApi.getInfo()
                            .title("Carland — Admin Feature Flags")
                            .description("""
                                    PO feature-flag REST plus API catalog CRUD on **carland-service**.

                                    """ + adminNote);
                })
                .build();
    }

    @Bean
    public GroupedOpenApi partnerWebhookGroup() {
        return GroupedOpenApi.builder()
                .group("partner-webhooks")
                .displayName("3. Carland — Partner Webhook Receiver")
                .pathsToMatch("/webhook/**")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Carland — Partner Webhook Receiver")
                        .description("""
                                **carland-service** internal **receiver** for partner service-visit events (`/webhook/**`).

                                ### Relationship to Webhook Gateway
                                Partners should call **4. Webhook Gateway — Partner Edge Adapter**, not these endpoints
                                directly in production. The gateway validates/forwards (and can queue via RabbitMQ when
                                carland is down). These controllers are the **accepting** side inside carland.

                                Auth here is service-to-service (`X-Internal-Token` + partner `X-Signature`), not end-user JWT.
                                """))
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
                    param.setDescription(KONG_HEADER_BASE_DESC.getOrDefault(name, name));
                    param.setRequired(true);
                    param.setSchema(new StringSchema()
                            .readOnly(true)
                            .example("injected-by-kong-from-jwt"));
                    continue;
                }

                if ("Accept-Language".equalsIgnoreCase(name)) {
                    param.setDescription("Client locale. Fixed to `az` in Swagger UI (read-only).");
                    param.setRequired(true);
                    param.setSchema(new StringSchema()
                            ._default("az")
                            .example("az")
                            .readOnly(true));
                    continue;
                }

                if ("X-Client-Timezone".equalsIgnoreCase(name)) {
                    param.setDescription("Client timezone. Fixed to `Asia/Baku` in Swagger UI (read-only).");
                    param.setRequired(true);
                    param.setSchema(new StringSchema()
                            ._default("Asia/Baku")
                            .example("Asia/Baku")
                            .readOnly(true));
                    continue;
                }

                if ("X-App-Version".equalsIgnoreCase(name)) {
                    param.setDescription("App semver for `/me` and role-state snapshot. Unknown falls back to current.");
                    param.setSchema(new StringSchema().example("2.1.0"));
                }
            }
            return operation;
        };
    }
}
