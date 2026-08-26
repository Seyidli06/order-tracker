package com.ordertracker.auth.controller;

import com.ordertracker.auth.dto.AuthResponse;
import com.ordertracker.auth.dto.LoginRequest;
import com.ordertracker.auth.dto.RegisterRequest;
import com.ordertracker.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void shouldRegisterUserAndReturnCreated() {
        RegisterRequest request = new RegisterRequest(
                "user@test.com",
                "password123"
        );

        AuthResponse authResponse =
                new AuthResponse("jwt-token");

        when(authService.register(request))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("jwt-token", response.getBody().accessToken());

        verify(authService).register(request);
    }

    @Test
    void shouldLoginUserAndReturnOk() {
        LoginRequest request = new LoginRequest(
                "user@test.com",
                "password123"
        );

        AuthResponse authResponse =
                new AuthResponse("jwt-token");

        when(authService.login(request))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody().accessToken());

        verify(authService).login(request);
    }
}