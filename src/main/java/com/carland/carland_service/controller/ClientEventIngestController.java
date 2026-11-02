package com.carland.carland_service.controller;

import com.carland.carland_service.config.ClientEventProperties;
import com.carland.carland_service.service.clientevent.ClientEventIngestService;
import com.carland.carland_service.service.clientevent.ClientEventIngestService.IngestOutcome;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * tr: Mobil crash/error batch. Auth yoxdur. Uğur 204 və X-Accepted-Count.
 *     Kong bu path-i JWT-siz buraxmalıdır.
 * en: Mobile crash/error batch. No auth. Success is 204 plus X-Accepted-Count.
 *     Kong must allow this path without a JWT.
 */
@RestController
@RequiredArgsConstructor
public class ClientEventIngestController {

    private final ClientEventIngestService ingestService;
    private final ClientEventProperties properties;

    @SecurityRequirements
    @PostMapping("/api/v1/client-events")
    public ResponseEntity<Void> ingest(HttpServletRequest request) throws IOException {
        int max = Math.max(properties.getMaxBodyBytes(), 1);
        if (request.getContentLengthLong() > max) {
            return ResponseEntity.status(413).build();
        }
        byte[] body = request.getInputStream().readNBytes(max + 1);
        if (body.length > max) {
            return ResponseEntity.status(413).build();
        }
        IngestOutcome outcome = ingestService.ingest(body, clientIp(request));
        if (outcome.httpStatus() == 204) {
            return ResponseEntity.noContent()
                    .header("X-Accepted-Count", Integer.toString(outcome.accepted()))
                    .build();
        }
        return ResponseEntity.status(outcome.httpStatus()).build();
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
