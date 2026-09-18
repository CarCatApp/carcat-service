package com.carland.carland_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * tr: Booking staff isteklerinde Kong X-User-Id veya access JWT claim okur.
 * en: Reads Kong X-User-Id or access JWT claims for booking staff requests.
 */
@Component
public class BookingStaffRequestAuth {

    @Value("${access.token.secret-key}")
    private String accessTokenSecretKey;

    public Long userId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header != null && !header.isBlank()) {
            try {
                return Long.parseLong(header.trim());
            } catch (NumberFormatException ignored) {
                // fall through to JWT
            }
        }
        Claims claims = parseBearer(request);
        if (claims == null) {
            return null;
        }
        Object raw = claims.get("userId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw != null) {
            try {
                return Long.parseLong(raw.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public boolean mustChangePassword(HttpServletRequest request) {
        Claims claims = parseBearer(request);
        if (claims == null) {
            return false;
        }
        Object raw = claims.get("mustChangePassword");
        return Boolean.TRUE.equals(raw) || "true".equalsIgnoreCase(String.valueOf(raw));
    }

    private Claims parseBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(accessTokenSecretKey.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception ex) {
            return null;
        }
    }
}
