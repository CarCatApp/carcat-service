package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * tr: Bir hadisəni yoxlayır. Batch-in qalanını atmaq üçün null qaytarır.
 *     Ölçü həddini aşan stek və mesaj kəsilir, hadisə saxlanır. Məcburi sahə yoxdursa atılır.
 * en: Validates one event. Returns null so the rest of the batch can continue.
 *     Oversized stack and message are cut and the event is kept. A missing required field is dropped.
 */
public final class ClientEventParser {

    private static final Set<String> TYPES = Set.of("fatal", "nonFatal", "apiError");
    private static final Set<String> ENVIRONMENTS = Set.of("release", "profile");
    private static final Set<String> NETWORKS = Set.of("wifi", "mobile", "none", "unknown");
    private static final Instant EARLIEST = Instant.parse("2020-01-01T00:00:00Z");

    private ClientEventParser() {
    }

    public record Parsed(
            String eventId,
            String eventType,
            LocalDateTime occurredAt,
            String message,
            String stackTrace,
            String sessionId,
            String installId,
            Long userId,
            String appVersion,
            String buildNumber,
            String packageName,
            String osName,
            String osVersion,
            Long memoryRssBytes,
            String network,
            String environment,
            String contextLibrary,
            String contextRoute,
            Integer statusCode,
            String endpoint,
            String signal,
            List<Map<String, Object>> breadcrumbs
    ) {
    }

    public static Parsed parse(JsonNode node, ClientEventProperties limits, Clock clock) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String eventId = uuid(text(node.get("eventId")));
        String eventType = text(node.get("type"));
        if (eventId == null || eventType == null || !TYPES.contains(eventType)) {
            return null;
        }
        String message = clip(text(node.get("message")), limits.getMessageMax());
        if (message == null || message.isBlank()) {
            return null;
        }
        Instant occurred = parseTime(text(node.get("timestamp")));
        if (occurred == null) {
            return null;
        }
        Instant now = clock.instant();
        if (occurred.isAfter(now.plus(Duration.ofMinutes(5))) || occurred.isBefore(EARLIEST)) {
            occurred = now;
        }
        String sessionId = identity(text(node.get("sessionId")));
        String installId = identity(text(node.get("installId")));
        if (sessionId == null || installId == null) {
            return null;
        }
        String environment = text(node.get("environment"));
        if (environment == null || !ENVIRONMENTS.contains(environment)) {
            return null;
        }
        JsonNode app = node.get("app");
        JsonNode os = node.get("os");
        if (app == null || !app.isObject() || os == null || !os.isObject()) {
            return null;
        }
        String appVersion = clip(text(app.get("version")), 40);
        String buildNumber = clip(text(app.get("buildNumber")), 40);
        String packageName = clip(text(app.get("packageName")), 200);
        String osName = clip(text(os.get("name")), 32);
        String osVersion = clip(text(os.get("version")), 80);
        if (blank(appVersion) || blank(buildNumber) || blank(packageName) || blank(osName) || blank(osVersion)) {
            return null;
        }

        JsonNode context = node.get("context");
        String library = null;
        String route = null;
        Integer status = null;
        String endpoint = null;
        if (context != null && context.isObject()) {
            library = clip(text(context.get("library")), 64);
            route = clip(text(context.get("route")), 160);
            status = integer(context.get("statusCode"));
            endpoint = clip(text(context.get("endpoint")), 500);
        }
        String signal = null;
        if ("apiError".equals(eventType)) {
            signal = ClientEventFingerprint.apiSignal(message);
            if (status == null || status < 100 || status > 599) {
                status = ClientEventFingerprint.statusFromMessage(message);
            }
            if (status != null && (status < 100 || status > 599)) {
                status = null;
            }
            if (endpoint == null || endpoint.isBlank()) {
                endpoint = clip(ClientEventFingerprint.endpointFromMessage(message), 500);
            }
        } else if (status != null && (status < 100 || status > 599)) {
            status = null;
        }

        String stack = null;
        if (!"apiError".equals(eventType)) {
            stack = clip(text(node.get("stackTrace")), limits.getStackMax());
            if (stack != null && stack.isBlank()) {
                stack = null;
            }
        }

        return new Parsed(
                eventId,
                eventType,
                LocalDateTime.ofInstant(occurred, ZoneOffset.UTC),
                message,
                stack,
                sessionId,
                installId,
                userId(node.get("userId")),
                appVersion,
                buildNumber,
                packageName,
                osName,
                osVersion,
                memory(node.get("memoryRssBytes")),
                network(text(node.get("network"))),
                environment,
                library,
                route,
                status,
                endpoint == null || endpoint.isBlank() ? null : endpoint,
                signal,
                breadcrumbs(node.get("breadcrumbs"), limits.getBreadcrumbMax())
        );
    }

    private static List<Map<String, Object>> breadcrumbs(JsonNode node, int max) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (node == null || !node.isArray() || max <= 0) {
            return out;
        }
        for (JsonNode item : node) {
            if (out.size() == max) {
                break;
            }
            if (item == null || !item.isObject()) {
                continue;
            }
            String message = clip(text(item.get("message")), 300);
            String category = clip(text(item.get("category")), 60);
            String timestamp = clip(text(item.get("timestamp")), 40);
            if (blank(message) && blank(category)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", timestamp);
            row.put("category", category);
            row.put("message", message);
            out.add(row);
        }
        return out;
    }

    private static Long userId(JsonNode node) {
        Long value = longValue(node);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private static Long memory(JsonNode node) {
        Long value = longValue(node);
        if (value == null || value < 0) {
            return null;
        }
        return value;
    }

    private static Long longValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Integer integer(JsonNode node) {
        Long value = longValue(node);
        if (value == null || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private static String network(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return NETWORKS.contains(value) ? value : null;
    }

    private static String uuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Instant parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(raw.trim()).toInstant();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber()) {
            return node.asText();
        }
        return null;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    /** tr: Kimlik kəsilmir. 64 simvoldan uzundursa hadisə atılır. */
    private static String identity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            return null;
        }
        return trimmed;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
