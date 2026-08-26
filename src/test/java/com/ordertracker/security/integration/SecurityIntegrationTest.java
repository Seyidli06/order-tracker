package com.ordertracker.security.integration;

import com.ordertracker.security.config.SecurityConfig;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityIntegrationTest.TestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldAllowWebhookEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/webhooks/test")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/test/protected")
                )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldAllowProtectedEndpointForAuthenticatedUser() throws Exception {
        mockMvc.perform(
                        get("/api/test/protected")
                                .with(user("user@test.com")
                                        .roles("USER"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAuditEndpointForUserRole() throws Exception {
        mockMvc.perform(
                        get("/api/audit/test")
                                .with(user("user@test.com")
                                        .roles("USER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAuditEndpointForAdminRole() throws Exception {
        mockMvc.perform(
                        get("/api/audit/test")
                                .with(user("admin@test.com")
                                        .roles("ADMIN"))
                )
                .andExpect(status().isOk());
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