package com.helper.server.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256!!";

    private final JwtUtil jwt = new JwtUtil(SECRET);

    @Test
    void issuedTokenValidatesBackToTheUser() {
        String token = jwt.issue("pbrchnk", Duration.ofHours(3));

        assertThat(jwt.validateAndGetUser(token)).isEqualTo("pbrchnk");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwt.issue("pbrchnk", Duration.ofSeconds(-5));

        assertThatThrownBy(() -> jwt.validateAndGetUser(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = new JwtUtil("another-secret-that-is-long-enough-for-hs256").issue("pbrchnk", Duration.ofHours(1));

        assertThatThrownBy(() -> jwt.validateAndGetUser(foreign)).isInstanceOf(SignatureException.class);
    }
}
