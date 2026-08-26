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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                SecurityIntegrationTest.TestController.class
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

    @RestController
    static class TestController {

        @GetMapping("/api/webhooks/test")
        String webhook() {
            return "webhook";
        }

        @GetMapping("/api/test/protected")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping("/api/audit/test")
        String audit() {
            return "audit";
        }
    }
}