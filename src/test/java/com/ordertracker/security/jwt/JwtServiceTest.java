package com.ordertracker.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        String rawSecret =
                "01234567890123456789012345678901";

        String base64Secret = Base64.getEncoder()
                .encodeToString(
                        rawSecret.getBytes(StandardCharsets.UTF_8)
                );

        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                base64Secret
        );

        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpiration",
                3_600_000L
        );

        userDetails = User.builder()
                .username("user@test.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void shouldGenerateToken() {
        String token =
                jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token =
                jwtService.generateToken(userDetails);

        String username =
                jwtService.extractUsername(token);

        assertEquals(
                "user@test.com",
                username
        );
    }

    @Test
    void shouldReturnTrueForValidToken() {
        String token =
                jwtService.generateToken(userDetails);

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        userDetails
                );

        assertTrue(valid);
    }

    @Test
    void shouldReturnFalseWhenTokenBelongsToDifferentUser() {
        String token =
                jwtService.generateToken(userDetails);

        UserDetails anotherUser = User.builder()
                .username("another@test.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        anotherUser
                );

        assertFalse(valid);
    }
}