package com.carland.carland_service.service.webhook;

import com.carland.carland_service.exceptions.ResponseException;
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

    public void writeUnauthorized(HttpServletResponse response, WebhookAuthFailure failure) throws IOException {
        ResponseException body = ResponseException.builder()
                .error(failure.getError())
                .message(failure.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build();
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, failure.name(), body);
    }

    private void writeJson(HttpServletResponse response, int status, String errorCode, Object body) throws IOException {
        if (response.isCommitted()) {
            log.error("Cannot write webhook auth JSON: response already committed, errorCode={}", errorCode);
            return;
        }

        byte[] jsonBytes = objectMapper.writeValueAsBytes(body);

        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setContentLength(jsonBytes.length);
        response.setHeader(AUTH_ERROR_HEADER, errorCode);
        response.getOutputStream().write(jsonBytes);
        response.getOutputStream().flush();
    }
}
