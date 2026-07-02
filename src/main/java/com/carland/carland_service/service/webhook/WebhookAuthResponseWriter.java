package com.carland.carland_service.service.webhook;

import com.carland.carland_service.exceptions.WebhookAuthErrorResponse;
import com.carland.carland_service.exceptions.WebhookAuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookAuthResponseWriter {

    public static final String AUTH_ERROR_HEADER = "X-Webhook-Auth-Error";

    private final ObjectMapper objectMapper;

    public void writeUnauthorized(HttpServletResponse response, WebhookAuthValidationResult result) throws IOException {
        WebhookAuthFailure failure = result.getFailure();
        WebhookAuthException exception = new WebhookAuthException(failure, result.getPartnerId());
        WebhookAuthErrorResponse body = WebhookAuthErrorResponse.builder()
                .error(failure.getError())
                .message(exception.getMessage())
                .partnerId(result.getPartnerId())
                .timeStamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build();
        writeJson(response, failure.name(), body);
    }

    public void writeUnauthorized(HttpServletResponse response, WebhookAuthFailure failure) throws IOException {
        writeUnauthorized(response, WebhookAuthValidationResult.failure(failure));
    }

    private void writeJson(HttpServletResponse response, String errorCode, WebhookAuthErrorResponse body) throws IOException {
        if (response.isCommitted()) {
            log.error("Cannot write webhook auth JSON: response already committed, errorCode={}", errorCode);
            return;
        }

        byte[] jsonBytes;
        try {
            jsonBytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            log.error("Failed to serialize webhook auth error as JSON, using fallback", ex);
            jsonBytes = fallbackJson(body).getBytes(StandardCharsets.UTF_8);
        }

        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(AUTH_ERROR_HEADER, errorCode);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    private static String fallbackJson(WebhookAuthErrorResponse body) {
        return "{\"error\":\"" + escape(body.getError())
                + "\",\"message\":\"" + escape(body.getMessage())
                + "\",\"partnerId\":" + (body.getPartnerId() != null ? body.getPartnerId() : "null")
                + ",\"status\":401}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
