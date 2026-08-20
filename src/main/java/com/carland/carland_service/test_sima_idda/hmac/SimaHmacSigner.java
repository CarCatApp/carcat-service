package com.carland.carland_service.test_sima_idda.hmac;

import com.carland.carland_service.test_sima_idda.config.SimaIddaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SIMA HMAC helper: minify JSON once, sign those exact bytes, send the same bytes as body.
 */
@Component
@RequiredArgsConstructor
public class SimaHmacSigner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SimaIddaProperties simaIddaProperties;

    public static String minify(Object body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to minify SIMA request body", e);
        }
    }

    public String signBase64(String minifiedBody) {
        String secret = simaIddaProperties.getSimaHmacSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("sima.hmac-secret / SIMA_HMAC_SECRET is not set");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(minifiedBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to HMAC-sign SIMA request body", e);
        }
    }
}
