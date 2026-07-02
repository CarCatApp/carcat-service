package com.carland.carland_service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacSignatureValidator {

    public static final String HEADER_NAME = "X-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public byte[] resolvePayload(HttpServletRequest request, byte[] body) {
        if (body != null && body.length > 0) {
            return body;
        }
        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            return queryString.getBytes(StandardCharsets.UTF_8);
        }
        return new byte[0];
    }

    public boolean isValid(String secret, byte[] payload, String providedSignature) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(providedSignature)) {
            return false;
        }
        byte[] expected = computeMac(secret, payload);
        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(providedSignature.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, provided);
    }

    /** First 16 hex chars of SHA-256(payload) — for signature debugging in logs only. */
    public String sha256Prefix(byte[] payload) {
        byte[] data = payload != null ? payload : new byte[0];
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }

    private byte[] computeMac(String secret, byte[] payload) {
        byte[] data = payload != null ? payload : new byte[0];
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }
}
