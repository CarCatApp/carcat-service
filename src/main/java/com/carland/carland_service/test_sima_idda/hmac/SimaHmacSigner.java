package com.carland.carland_service.test_sima_idda.hmac;

import com.carland.carland_service.test_sima_idda.config.SimaIddaConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SIMA HMAC helper: minify JSON once, sign those exact bytes, send the same bytes as body.
 * Same trap family as HyperService signing.
 */
public final class SimaHmacSigner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SimaHmacSigner() {
    }

    /** Serialize with no pretty-print (compact JSON). */
    public static String minify(Object body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to minify SIMA request body", e);
        }
    }

    /** HMAC-SHA256(secret, minifiedBody) → Base64 for Signature header. */
    public static String signBase64(String minifiedBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    SimaIddaConstants.EXAMPLE_SIMA_HMAC_SECRET.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] raw = mac.doFinal(minifiedBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to HMAC-sign SIMA request body", e);
        }
    }
}
