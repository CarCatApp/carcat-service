package com.carland.carland_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * tr: Admin panel JWT cookie (auth access token) okur/yazar; yalnız panel admin telefonunu kabul eder.
 * en: Reads/writes the admin-panel JWT cookie; accepts only the panel admin phone.
 */
@Component
public class AdminAccessService {

    public static final String COOKIE = "ADMIN_ACCESS";
    public static final String PANEL_PHONE = "+994500000000";

    @Value("${access.token.secret-key}")
    private String accessTokenSecretKey;

    public void writeCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie(COOKIE, accessToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/admin");
        cookie.setMaxAge(15 * 60);
        response.addCookie(cookie);
    }

    public void clearCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE, "");
        cookie.setPath("/admin");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public enum Status {
        MISSING,
        INVALID,
        FORBIDDEN,
        OK
    }

    public Status inspect(HttpServletRequest request) {
        String token = readToken(request);
        if (token == null || token.isBlank()) {
            return Status.MISSING;
        }
        Claims claims = parse(token);
        if (claims == null) {
            return Status.INVALID;
        }
        String phone = claims.getSubject();
        String role = stringClaim(claims, "role");
        if (PANEL_PHONE.equals(phone) && "ADMIN".equalsIgnoreCase(role)) {
            return Status.OK;
        }
        return Status.FORBIDDEN;
    }

    public boolean isPanelAdmin(HttpServletRequest request) {
        return inspect(request) == Status.OK;
    }

    public String actor(HttpServletRequest request) {
        Claims claims = parse(readToken(request));
        if (claims == null) {
            return "unknown";
        }
        return claims.getSubject();
    }

    private String readToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(accessTokenSecretKey.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception ex) {
            return null;
        }
    }

    private String stringClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value != null ? value.toString() : null;
    }
}
