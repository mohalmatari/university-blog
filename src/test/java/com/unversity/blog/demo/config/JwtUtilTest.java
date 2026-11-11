package com.unversity.blog.demo.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class JwtUtilTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("0123456789-0123456789-0123456789-01".getBytes(StandardCharsets.UTF_8));

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // Runs before EVERY test, so each test gets a fresh JwtUtil.
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);

        jwtUtil.init();
    }

    // tests

    @Test
    @DisplayName("a generated token contains the username it was created for")
    void generateToken_thenExtractUsername_returnsSameUsername() {
        // Act
        String token = jwtUtil.generateToken("mohammed");
        String extracted = jwtUtil.extractUsername(token);

        // Assert
        assertEquals("mohammed", extracted);
    }

    @Test
    @DisplayName("isTokenValid returns true when the username matches")
    void isTokenValid_withSameUsername_returnsTrue() {
        String token = jwtUtil.generateToken("mohammed");

        assertTrue(jwtUtil.isTokenValid(token, "mohammed"));
    }

    @Test
    @DisplayName("isTokenValid returns false when the token belongs to someone else")
    void isTokenValid_withDifferentUsername_returnsFalse() {
        String token = jwtUtil.generateToken("mohammed");

        assertFalse(jwtUtil.isTokenValid(token, "someone_else"));
    }

    @Test
    @DisplayName("a token with a changed signature is rejected")
    void extractUsername_withTamperedToken_throwsJwtException() {
        String token = jwtUtil.generateToken("mohammed");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtUtil.extractUsername(tampered));
    }

    @Test
    @DisplayName("random text is rejected as a token")
    void extractUsername_withGarbage_throwsJwtException() {
        assertThrows(JwtException.class, () -> jwtUtil.extractUsername("not.a.jwt"));
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void extractUsername_withTokenFromOtherKey_throwsJwtException() {
        String forged = buildToken("mohammed", otherKey(), minutesFromNow(10));

        assertThrows(JwtException.class, () -> jwtUtil.extractUsername(forged));
    }

    @Test
    @DisplayName("an expired token is rejected with ExpiredJwtException")
    void extractUsername_withExpiredToken_throwsExpiredJwtException() {
        String expired = buildToken("mohammed", ourKey(), minutesFromNow(-1));

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(expired));
    }

    // helpers

    private SecretKey ourKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private SecretKey otherKey() {
        return Keys.hmacShaKeyFor("ZZZZZZZZZZ-ZZZZZZZZZZ-ZZZZZZZZZZ-ZZ".getBytes(StandardCharsets.UTF_8));
    }

    private Date minutesFromNow(int minutes) {
        return new Date(System.currentTimeMillis() + minutes * 60_000L);
    }

    private String buildToken(String username, SecretKey key, Date expiration) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}