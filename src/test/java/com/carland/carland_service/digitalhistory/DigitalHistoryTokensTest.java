package com.carland.carland_service.digitalhistory;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigitalHistoryTokensTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void shareTokenCarriesCarIdAndNoUserSession() {
        DigitalHistoryTokens tokens = new DigitalHistoryTokens(SECRET, "carland", 300, Clock.systemUTC());
        String token = tokens.sign(42L);

        assertEquals(42L, tokens.parseIssueId(token));
        String json = payload(token);
        assertTrue(json.contains("\"purpose\":\"DIGITAL_HISTORY\""));
        assertTrue(json.contains("\"issueId\":42"));
        assertFalse(json.contains("userId"));
        assertFalse(json.contains("role"));
    }

    @Test
    void expiredTokenIsRejected() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-05T09:00:00Z"));
        DigitalHistoryTokens tokens = new DigitalHistoryTokens(SECRET, "carland", 300, clock);
        String token = tokens.sign(7L);
        clock.instant = clock.instant.plusSeconds(301);

        DigitalHistoryTokenException ex = assertThrows(DigitalHistoryTokenException.class, () -> tokens.parseIssueId(token));
        assertTrue(ex.isExpired());
    }

    @Test
    void userAccessTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String access = Jwts.builder()
                .subject("0501112233")
                .claim("userId", 9L)
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .issuer("carland")
                .signWith(key)
                .compact();
        DigitalHistoryTokens tokens = new DigitalHistoryTokens(SECRET, "carland", 300, Clock.systemUTC());

        DigitalHistoryTokenException ex = assertThrows(
                DigitalHistoryTokenException.class, () -> tokens.parseIssueId(access));
        assertFalse(ex.isExpired());
    }

    @Test
    void pdfPathIsOnlyTheBrowserGet() {
        assertTrue(DigitalHistoryPaths.isPdf("/api/v1/car/digital-history"));
        assertTrue(DigitalHistoryPaths.isPdf("/carland/server-carland/api/v1/car/digital-history"));
        assertFalse(DigitalHistoryPaths.isPdf("/api/v1/car/15/digital-history/link"));
        assertFalse(DigitalHistoryPaths.isPdf("/api/v1/car/digital-history/verify/CC-2026-K7Q2M9X4TA"));
        assertFalse(DigitalHistoryPaths.isPdf(null));
        assertTrue(DigitalHistoryPaths.isVerify("/verify/CC-2026-K7Q2M9X4TA"));
        assertTrue(DigitalHistoryPaths.isVerify("/verify/CC-2026-K7Q2M9X4TA/pdf"));
        assertTrue(DigitalHistoryPaths.isVerify("/api/v1/car/digital-history/verify/CC-2026-K7Q2M9X4TA"));
        assertFalse(DigitalHistoryPaths.isVerify("/api/v1/car/digital-history"));
        assertFalse(DigitalHistoryPaths.isVerify("/api/v1/car/15/digital-history/link"));
    }

    private static String payload(String token) {
        String body = token.split("\\.")[1];
        int pad = (4 - body.length() % 4) % 4;
        return new String(Base64.getUrlDecoder().decode(body + "=".repeat(pad)));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
