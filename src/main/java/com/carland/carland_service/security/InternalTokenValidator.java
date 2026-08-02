package com.carland.carland_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * tr: Servisler arası webhook isteklerinde X-Internal-Token header'ını carland.internal-token
 *     konfigürasyonu ile sabit zamanlı karşılaştırarak doğrulayan bileşendir.
 * en: Component validating the X-Internal-Token header of service-to-service webhook requests
 *     against the carland.internal-token configuration using constant-time comparison.
 */
@Component
public class InternalTokenValidator {

    public static final String HEADER_NAME = "X-Internal-Token";

    private final String expectedToken;

    public InternalTokenValidator(@Value("${carland.internal-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    /**
     * tr: Token'ı doğrular: konfigürasyon boşsa INTERNAL_TOKEN_NOT_CONFIGURED, header yoksa
     *     MISSING_INTERNAL_TOKEN, eşleşmezse INVALID_INTERNAL_TOKEN hatası; aksi halde başarı döner.
     * en: Validates the token: returns INTERNAL_TOKEN_NOT_CONFIGURED when config is blank,
     *     MISSING_INTERNAL_TOKEN when the header is absent, INVALID_INTERNAL_TOKEN on mismatch;
     *     success otherwise.
     */
    public WebhookAuthValidationResult validate(HttpServletRequest request) {
        if (!StringUtils.hasText(expectedToken)) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.INTERNAL_TOKEN_NOT_CONFIGURED);
        }
        String provided = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(provided)) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.MISSING_INTERNAL_TOKEN);
        }
        if (!MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.INVALID_INTERNAL_TOKEN);
        }
        return WebhookAuthValidationResult.success();
    }
}
