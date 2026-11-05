package com.carland.carland_service.digitalhistory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * tr: Dijital geçmiş paylaşım JWT'si. Access token ile aynı sır ve aynı {@code iss}.
 *     İçinde {@code userId} ve {@code role} yoktur. Süre {@code digital-history.share-seconds}.
 * en: Digital history share JWT. Same secret and {@code iss} as the access token.
 *     It carries no {@code userId} and no {@code role}. Lifetime is {@code digital-history.share-seconds}.
 */
@Component
public class DigitalHistoryTokens {

    public static final String PURPOSE = "DIGITAL_HISTORY";

    private final SecretKey key;
    private final String issuer;
    private final long shareSeconds;
    private final Clock clock;

    @Autowired
    public DigitalHistoryTokens(
            @Value("${access.token.secret-key}") String secret,
            @Value("${carland.issuer.key}") String issuer,
            @Value("${digital-history.share-seconds:300}") long shareSeconds) {
        this(secret, issuer, shareSeconds, Clock.systemUTC());
    }

    DigitalHistoryTokens(String secret, String issuer, long shareSeconds, Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.shareSeconds = shareSeconds;
        this.clock = clock;
    }

    public long shareSeconds() {
        return shareSeconds;
    }

    public String sign(long carId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject("digital-history")
                .claim("purpose", PURPOSE)
                .claim("carId", carId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(shareSeconds)))
                .issuer(issuer)
                .signWith(key)
                .compact();
    }

    public long parseCarId(String token) {
        if (token == null || token.isBlank() || token.length() > 4000) {
            throw DigitalHistoryTokenException.invalid();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            Object raw = claims.get("carId");
            String purpose = claims.get("purpose", String.class);
            if (!PURPOSE.equals(purpose) || !(raw instanceof Number number) || number.longValue() <= 0) {
                throw DigitalHistoryTokenException.invalid();
            }
            return number.longValue();
        } catch (ExpiredJwtException ex) {
            throw DigitalHistoryTokenException.expired();
        } catch (DigitalHistoryTokenException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw DigitalHistoryTokenException.invalid();
        }
    }
}
