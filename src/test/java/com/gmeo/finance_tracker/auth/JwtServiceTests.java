package com.gmeo.finance_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTests {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-for-jwt-token-generation-at-least-32-characters");
        jwtProperties.setAccessTokenExpirationMs(3600000);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generateAccessTokenCreatesValidToken() {
        String token = jwtService.generateAccessToken("test@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void extractEmailReturnsTokenSubject() {
        String token = jwtService.generateAccessToken("test@example.com");

        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        assertThat(jwtService.validateToken("not-a-valid-token")).isFalse();
    }
}
