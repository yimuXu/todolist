package com.example.jira.Auth;

import com.example.jira.TestEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The token is the only thing standing between a request and somebody else's to-do lists, so the
 * cases that matter are the negative ones: a token that names a different user, a token signed
 * with a different secret, and a token that has already expired.
 */
class JwtUtilTest {

    private static final String SECRET = "jira-app-test-secret-key-at-least-32-chars-long";

    private JwtUtil jwtUtil;

    private static JwtUtil jwtUtil(String secret, long expiryMillis) {
        JwtUtil util = new JwtUtil();
        TestEntities.withField(util, "secret", secret);
        TestEntities.withField(util, "expiration", expiryMillis);
        return util;
    }

    @BeforeEach
    void setUp() {
        jwtUtil = jwtUtil(SECRET, 86_400_000L);
    }

    @Test
    void aFreshTokenNamesTheUserItWasIssuedFor() {
        String token = jwtUtil.generateToken("dd");

        assertEquals("dd", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token, "dd"));
    }

    @Test
    void aTokenIsNotValidForSomebodyElse() {
        String token = jwtUtil.generateToken("dd");

        assertFalse(jwtUtil.validateToken(token, "mallory"));
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        String forged = jwtUtil("a-completely-different-secret-key-32-chars", 86_400_000L)
                .generateToken("dd");

        assertFalse(jwtUtil.validateToken(forged, "dd"));
    }

    @Test
    void anExpiredTokenIsRejected() throws InterruptedException {
        String token = jwtUtil(SECRET, 1L).generateToken("dd");
        Thread.sleep(1_100);

        assertFalse(jwtUtil.validateToken(token, "dd"));
    }

    @Test
    void rubbishIsRejectedRatherThanThrown() {
        assertFalse(jwtUtil.validateToken("not-a-token", "dd"));
        assertFalse(jwtUtil.validateToken("", "dd"));
    }
}
