package com.carland.carland_service.security;

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

/**
 * tr: Webhook isteklerinin HMAC-SHA256 imzasını doğrulayan bileşendir; imzalanacak payload'ı çözer
 *     (body veya sıralı query parametreleri) ve X-Signature header'ındaki hex imzayla sabit zamanlı karşılaştırır.
 * en: Component validating the HMAC-SHA256 signature of webhook requests; resolves the payload to sign
 *     (body or sorted query parameters) and compares it with the hex signature from the X-Signature header
 *     in constant time.
 */
@Component
public class HmacSignatureValidator {

    public static final String HEADER_NAME = "X-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

//    public byte[] resolvePayload(HttpServletRequest request, byte[] body) {
//        if (body != null && body.length > 0) {
//            return body;
//        }
//        String queryString = request.getQueryString();
//        if (StringUtils.hasText(queryString)) {
//            return queryString.getBytes(StandardCharsets.UTF_8);
//        }
//        return new byte[0];
//    }
//public byte[] resolvePayload(HttpServletRequest request, byte[] body) {
//    if (body != null && body.length > 0) {
//        return body;
//    }
//
//    if ("GET".equalsIgnoreCase(request.getMethod())) {
//        StringBuilder payload = new StringBuilder(request.getRequestURL());
//
//        String queryString = request.getQueryString();
//        if (StringUtils.hasText(queryString)) {
//            payload.append('?').append(queryString);
//        }
//
//        return payload.toString().getBytes(StandardCharsets.UTF_8);
//    }
//
//    return new byte[0];
//}

    /**
     * tr: İmzalanacak payload'ı belirler: body doluysa body'yi, boşsa alfabetik sıralanmış ve URL-encode
     *     edilmiş query parametrelerini döner; ikisi de yoksa boş dizi döner.
     * en: Resolves the payload to sign: returns the body when present, otherwise the alphabetically sorted
     *     and URL-encoded query parameters; returns an empty array when neither exists.
     */
    public byte[] resolvePayload(HttpServletRequest request, byte[] body) {
        if (body != null && body.length > 0) {
            return body;
        }

        if (!request.getParameterMap().isEmpty()) {
            String payload = request.getParameterMap().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .flatMap(entry -> java.util.Arrays.stream(entry.getValue())
                            .map(value -> entry.getKey() + "=" +
                                    java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)))
                    .collect(java.util.stream.Collectors.joining("&"));

            return payload.getBytes(StandardCharsets.UTF_8);
        }

        return new byte[0];
    }

    /**
     * tr: Verilen secret ile payload'ın HMAC'ini hesaplayıp sağlanan hex imzayla karşılaştırır;
     *     secret/imza boşsa veya hex geçersizse false döner. Timing-attack'e karşı sabit zamanlı kıyas kullanır.
     * en: Computes the payload HMAC with the given secret and compares it to the provided hex signature;
     *     returns false when secret/signature is blank or hex is invalid. Uses constant-time comparison
     *     against timing attacks.
     */
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

    /**
     * tr: Payload'ın SHA-256 özetinin ilk 16 hex karakterini döner — sadece log'larda imza hata ayıklaması için.
     * en: First 16 hex chars of SHA-256(payload) — for signature debugging in logs only.
     */
    public String sha256Prefix(byte[] payload) {
        byte[] data = payload != null ? payload : new byte[0];
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }

    /**
     * tr: HMAC-SHA256 değerini hesaplar; algoritma/anahtar hatasında IllegalStateException fırlatır.
     * en: Computes the HMAC-SHA256 value; throws IllegalStateException on algorithm/key failure.
     */
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
