package com.carland.carland_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingStaffRequestAuthTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final BookingStaffRequestAuth auth = new BookingStaffRequestAuth();

    @BeforeEach
    void secret() {
        ReflectionTestUtils.setField(auth, "accessTokenSecretKey", SECRET);
    }

    @Test
    void bearerUserIdIgnoresTheUserIdHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token(12L));
        request.addHeader("X-User-Id", "99");

        assertEquals(12L, auth.userIdFromBearer(request));
    }

    @Test
    void aBrokenBearerIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-token");

        assertNull(auth.userIdFromBearer(request));
    }

    private static String token(long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().claim("userId", userId).signWith(key).compact();
    }
}
