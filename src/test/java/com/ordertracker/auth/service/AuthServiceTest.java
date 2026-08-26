package com.ordertracker.auth.service;

import com.ordertracker.auth.dto.AuthResponse;
import com.ordertracker.auth.dto.LoginRequest;
import com.ordertracker.auth.dto.RegisterRequest;
import com.ordertracker.common.enums.Role;
import com.ordertracker.security.jwt.JwtService;
import com.ordertracker.user.entity.User;
import com.ordertracker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ordertracker.exception.ResourceConflictException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                userDetailsService,
                jwtService
        );
    }

    @Test
    void shouldRegisterUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest(
                "user@test.com",
                "password123"
        );

        when(userRepository.existsByEmail("user@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.accessToken());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("user@test.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());

        verify(passwordEncoder).encode("password123");
        verify(userDetailsService)
                .loadUserByUsername("user@test.com");
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "user@test.com",
                "password123"
        );

        when(userRepository.existsByEmail("user@test.com"))
                .thenReturn(true);

        ResourceConflictException exception =
                assertThrows(
                        ResourceConflictException.class,
                        () -> authService.register(request)
                );

        assertEquals(
                "User with this email already exists",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldLoginAndReturnToken() {
        LoginRequest request = new LoginRequest(
                "user@test.com",
                "password123"
        );

        when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.accessToken());

        verify(authenticationManager)
                .authenticate(
                        any(UsernamePasswordAuthenticationToken.class)
                );

        verify(userDetailsService)
                .loadUserByUsername("user@test.com");

        verify(jwtService)
                .generateToken(userDetails);
    }
}