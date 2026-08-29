package com.ordertracker.security.integration;

import com.ordertracker.security.config.SecurityConfig;
import com.ordertracker.security.handler.RestAccessDeniedHandler;
import com.ordertracker.security.handler.RestAuthenticationEntryPoint;
import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import com.ordertracker.security.jwt.JwtAuthenticationFilter;
import com.ordertracker.security.jwt.JwtService;
import com.ordertracker.security.service.CustomUserDetailsService;
import com.ordertracker.audit.AuditService;
import com.ordertracker.audit.WebhookAuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = {TestSecurityController.class, com.ordertracker.audit.controller.AuditLogController.class}
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

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        WebhookAuditLog auditLog = WebhookAuditLog.builder()
                .id(1L)
                .eventId("evt_test_123")
                .eventType("PAYMENT_SUCCEEDED")
                .source("stripe")
                .payload("{\"test\":\"payload\"}")
                .receivedAt(Instant.now())
                .processingStatus(WebhookAuditLog.ProcessingStatus.PROCESSED)
                .build();

        when(auditService.findByEventId("evt_test_123")).thenReturn(auditLog);
        when(auditService.findByStatus(WebhookAuditLog.ProcessingStatus.FAILED)).thenReturn(List.of());
    }

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
    void shouldReturnUnauthorizedForAuditWebhookEndpointWithoutAuth()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/webhooks/evt_test_123")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturnForbiddenForAuditWebhookEndpointWithUserRole()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/webhooks/evt_test_123")
                                .with(
                                        user("user@test.com")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldAllowAuditWebhookEndpointWithAdminRole()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/webhooks/evt_test_123")
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
    void shouldReturnUnauthorizedForAuditWebhookListEndpointWithoutAuth()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/webhooks?status=FAILED")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturnForbiddenForAuditWebhookListEndpointWithUserRole()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/webhooks?status=FAILED")
                                .with(
                                        user("user@test.com")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldAllowAuditWebhookListEndpointWithAdminRole()
            throws Exception {

        mockMvc.perform(
                        get("/api/audit/webhooks?status=FAILED")
                                .with(
                                        user("admin@test.com")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }


}