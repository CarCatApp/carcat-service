package com.carland.carland_service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Rewrites /v3/api-docs/swagger-config to the curated definition list and preserves
 * the reverse-proxy context prefix on URLs (e.g. /carland-docs).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class SwaggerConfigRewriteFilter extends OncePerRequestFilter {

    private static final String AUTH_LEGACY = "1A. Auth — Legacy (/users)";
    private static final String AUTH_NEW = "1B. Auth — NewUsers";
    private static final String MOBILE = "2. Carland — Mobile API";
    private static final String RECEIVER = "3. Carland — Partner Webhook Receiver";
    private static final String GATEWAY = "4. Webhook Gateway — Partner Edge Adapter";

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.endsWith("/v3/api-docs/swagger-config");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapped);

        byte[] body = wrapped.getContentAsByteArray();
        if (body.length == 0) {
            wrapped.copyBodyToResponse();
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            if (!(root instanceof ObjectNode objectNode)) {
                wrapped.copyBodyToResponse();
                return;
            }

            String prefix = resolveUrlPrefix(objectNode, request);
            ArrayNode urls = objectMapper.createArrayNode();
            for (Definition def : definitions()) {
                ObjectNode entry = objectMapper.createObjectNode();
                entry.put("name", def.name());
                entry.put("url", prefix + def.path());
                urls.add(entry);
            }
            objectNode.set("urls", urls);
            objectNode.put("urls.primaryName", AUTH_LEGACY);
            objectNode.put("persistAuthorization", true);

            byte[] rewritten = objectMapper.writeValueAsBytes(objectNode);
            response.resetBuffer();
            response.setStatus(wrapped.getStatus());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.setContentLength(rewritten.length);
            response.getOutputStream().write(rewritten);
            response.flushBuffer();
        } catch (Exception ex) {
            wrapped.copyBodyToResponse();
        }
    }

    private static String resolveUrlPrefix(ObjectNode objectNode, HttpServletRequest request) {
        JsonNode originalUrls = objectNode.get("urls");
        if (originalUrls != null && originalUrls.isArray()) {
            for (JsonNode item : originalUrls) {
                String sample = item.path("url").asText("");
                int idx = sample.indexOf("/v3/api-docs");
                if (idx > 0) {
                    return sample.substring(0, idx);
                }
            }
        }
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isBlank()) {
            return ctx;
        }
        String forwardedPrefix = request.getHeader("X-Forwarded-Prefix");
        if (forwardedPrefix != null && !forwardedPrefix.isBlank()) {
            return forwardedPrefix.endsWith("/")
                    ? forwardedPrefix.substring(0, forwardedPrefix.length() - 1)
                    : forwardedPrefix;
        }
        return "";
    }

    private static List<Definition> definitions() {
        return List.of(
                new Definition(AUTH_LEGACY, "/v3/api-docs/external/carland-auth"),
                new Definition(AUTH_NEW, "/v3/api-docs/external/carland-auth-new"),
                new Definition(MOBILE, "/v3/api-docs/mobile-api"),
                new Definition(RECEIVER, "/v3/api-docs/partner-webhooks"),
                new Definition(GATEWAY, "/v3/api-docs/external/webhook")
        );
    }

    private record Definition(String name, String path) {}
}
