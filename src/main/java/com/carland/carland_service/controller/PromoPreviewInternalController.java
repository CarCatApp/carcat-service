package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.PromoPreviewResponse;
import com.carland.carland_service.security.InternalTokenValidator;
import com.carland.carland_service.service.PromoPreviewService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: carland_auth, giriş öncesi promo cümlesi için çağırır. X-Internal-Token. Mobil buraya gelmez.
 * en: carland_auth calls this for the pre-login promo sentence. X-Internal-Token. Mobile does not call it.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class PromoPreviewInternalController {

    private final InternalTokenValidator tokens;
    private final PromoPreviewService preview;

    @GetMapping("/api/v1/internal/promo/preview")
    public ResponseEntity<PromoPreviewResponse> preview(HttpServletRequest request,
                                                        @RequestParam String phoneNumber,
                                                        @RequestParam String promoCode) {
        if (!tokens.validate(request).isValid()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(preview.preview(phoneNumber, promoCode));
    }
}
