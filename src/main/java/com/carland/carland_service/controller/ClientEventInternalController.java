package com.carland.carland_service.controller;

import com.carland.carland_service.security.InternalTokenValidator;
import com.carland.carland_service.service.clientevent.ClientEventMaintenance;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: carland_auth hesabı DELETED edəndə çağırır. Hadisə qalır, userId boşalır.
 * en: Called by carland_auth when an account becomes DELETED. The event stays, userId is cleared.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class ClientEventInternalController {

    private final InternalTokenValidator tokens;
    private final ClientEventMaintenance maintenance;

    @PostMapping("/api/v1/internal/client-events/anonymize")
    public ResponseEntity<Void> anonymize(HttpServletRequest request, @RequestParam Long userId) {
        if (!tokens.validate(request).isValid()) {
            return ResponseEntity.status(401).build();
        }
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        maintenance.anonymize(userId);
        return ResponseEntity.noContent().build();
    }
}
