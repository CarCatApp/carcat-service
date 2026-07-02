package com.carland.carland_service.service.webhook;

import com.carland.carland_service.exceptions.ResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WebhookAuthResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeUnauthorized(HttpServletResponse response, WebhookAuthFailure failure) throws IOException {
        ResponseException body = ResponseException.builder()
                .error(failure.getError())
                .message(failure.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build();
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, body);
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
        response.flushBuffer();
    }
}
