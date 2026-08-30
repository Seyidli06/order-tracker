package com.ordertracker.security.integration;

import com.ordertracker.security.config.SecurityConfig;
import com.ordertracker.security.handler.RestAccessDeniedHandler;
import com.ordertracker.security.handler.RestAuthenticationEntryPoint;
import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import com.ordertracker.security.jwt.JwtAuthenticationFilter;
import com.ordertracker.security.jwt.JwtService;
import com.ordertracker.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("security-test")
@WebMvcTest(
        controllers = TestSecurityController.class
)

@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityErrorResponseWriter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})


class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldAllowWebhookEndpointWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/webhooks/test")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldReturnUnauthorizedJsonWhenAuthenticationIsMissing()
            throws Exception {

        mockMvc.perform(
                        get("/api/test/protected")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Authentication is required"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/test/protected"
                                )
                )
                .andExpect(
                        jsonPath("$.validationErrors")
                                .isMap()
                );
    }

    @Test
    void shouldReturnUnauthorizedJsonForInvalidJwt()
            throws Exception {

        when(
                jwtService.extractUsername(
                        "invalid-token"
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Invalid JWT"
                )
        );

        mockMvc.perform(
                        get("/api/test/protected")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Authentication is required"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/test/protected"
                                )
                );
    }

    @Test
    void shouldAllowProtectedEndpointForAuthenticatedUser()
            throws Exception {

        mockMvc.perform(
                        get("/api/test/protected")
                                .with(
                                        user("user@test.com")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldReturnForbiddenJsonForUserRoleOnAuditEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/test")
                                .with(
                                        user("user@test.com")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Forbidden")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Access denied")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/audit/test"
                                )
                )
                .andExpect(
                        jsonPath("$.validationErrors")
                                .isMap()
                );
    }

    @Test
    void shouldAllowAuditEndpointForAdminRole()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/test")
                                .with(
                                        user("admin@test.com")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldReturnNotFoundJsonForUnknownEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/does-not-exist")
                                .with(
                                        user("user@test.com")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Resource not found")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/does-not-exist")
                );
    }

    @Test
    void shouldAllowLoginWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldAllowRegisterWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldRequireAuthenticationForOtherAuthEndpoints()
            throws Exception {

        mockMvc.perform(
                        get("/api/auth/test")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Authentication is required"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/auth/test"
                                )
                );
    }

}