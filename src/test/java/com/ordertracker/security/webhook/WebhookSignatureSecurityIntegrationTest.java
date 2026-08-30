package com.ordertracker.security.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.security.config.SecurityConfig;
import com.ordertracker.security.handler.RestAccessDeniedHandler;
import com.ordertracker.security.handler.RestAuthenticationEntryPoint;
import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import com.ordertracker.security.jwt.JwtAuthenticationFilter;
import com.ordertracker.security.jwt.JwtService;
import com.ordertracker.security.ratelimit.RateLimitFilter;
import com.ordertracker.security.service.CustomUserDetailsService;
import com.ordertracker.support.TestDataFactory;
import com.ordertracker.webhook.controller.WebhookController;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import com.ordertracker.webhook.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static com.ordertracker.support.WebhookSignatureTestUtils.PAYMENT_SECRET;
import static com.ordertracker.support.WebhookSignatureTestUtils.SHIPMENT_SECRET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WebhookController.class
)
@ActiveProfiles("test")
@EnableConfigurationProperties(
        WebhookSignatureProperties.class
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RateLimitFilter.class,
        WebhookSignatureFilter.class,
        WebhookSignatureVerifier.class,
        SecurityErrorResponseWriter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class WebhookSignatureSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebhookService webhookService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService
            customUserDetailsService;

    @Test
    void validPaymentSignature_ShouldReachController()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        String timestamp =
                currentTimestamp();

        String signature =
                sign(
                        PAYMENT_SECRET,
                        timestamp,
                        body
                );

        mockMvc.perform(
                        post("/api/webhooks/payment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        timestamp
                                )
                                .header(
                                        WebhookSignatureFilter.SIGNATURE_HEADER,
                                        signature
                                )
                                .content(body)
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                webhookService
        ).processPaymentWebhook(
                any(PaymentWebhookPayload.class),
                anyString()
        );
    }

    @Test
    void validShipmentSignature_ShouldReachController()
            throws Exception {

        ShipmentWebhookPayload payload =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "SHIPPED"
                        );

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        String timestamp =
                currentTimestamp();

        String signature =
                sign(
                        SHIPMENT_SECRET,
                        timestamp,
                        body
                );

        mockMvc.perform(
                        post("/api/webhooks/shipment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        timestamp
                                )
                                .header(
                                        WebhookSignatureFilter.SIGNATURE_HEADER,
                                        signature
                                )
                                .content(body)
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                webhookService
        ).processShipmentWebhook(
                any(ShipmentWebhookPayload.class),
                anyString()
        );
    }

    @Test
    void missingSignature_ShouldReturnUnauthorized()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        mockMvc.perform(
                        post("/api/webhooks/payment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        currentTimestamp()
                                )
                                .content(body)
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid webhook signature"
                                )
                );

        verify(
                webhookService,
                never()
        ).processPaymentWebhook(
                any(),
                anyString()
        );
    }

    @Test
    void wrongSignature_ShouldReturnUnauthorized()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        mockMvc.perform(
                        post("/api/webhooks/payment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        currentTimestamp()
                                )
                                .header(
                                        WebhookSignatureFilter.SIGNATURE_HEADER,
                                        "sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                )
                                .content(body)
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                webhookService,
                never()
        ).processPaymentWebhook(
                any(),
                anyString()
        );
    }

    @Test
    void expiredTimestamp_ShouldReturnUnauthorized()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .minusSeconds(301)
                                .getEpochSecond()
                );

        String signature =
                sign(
                        PAYMENT_SECRET,
                        timestamp,
                        body
                );

        mockMvc.perform(
                        post("/api/webhooks/payment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        timestamp
                                )
                                .header(
                                        WebhookSignatureFilter.SIGNATURE_HEADER,
                                        signature
                                )
                                .content(body)
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                webhookService,
                never()
        ).processPaymentWebhook(
                any(),
                anyString()
        );
    }

    @Test
    void modifiedBody_ShouldReturnUnauthorized()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        String originalBody =
                objectMapper.writeValueAsString(
                        payload
                );

        String timestamp =
                currentTimestamp();

        String signature =
                sign(
                        PAYMENT_SECRET,
                        timestamp,
                        originalBody
                );

        String modifiedBody =
                originalBody.replace(
                        payload.getEventId(),
                        payload.getEventId()
                                + "_tampered"
                );

        mockMvc.perform(
                        post("/api/webhooks/payment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        timestamp
                                )
                                .header(
                                        WebhookSignatureFilter.SIGNATURE_HEADER,
                                        signature
                                )
                                .content(modifiedBody)
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                webhookService,
                never()
        ).processPaymentWebhook(
                any(),
                anyString()
        );
    }

    @Test
    void paymentSecretForShipment_ShouldReturnUnauthorized()
            throws Exception {

        ShipmentWebhookPayload payload =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "SHIPPED"
                        );

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        String timestamp =
                currentTimestamp();

        String wrongSignature =
                sign(
                        PAYMENT_SECRET,
                        timestamp,
                        body
                );

        mockMvc.perform(
                        post("/api/webhooks/shipment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        WebhookSignatureFilter.TIMESTAMP_HEADER,
                                        timestamp
                                )
                                .header(
                                        WebhookSignatureFilter.SIGNATURE_HEADER,
                                        wrongSignature
                                )
                                .content(body)
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                webhookService,
                never()
        ).processShipmentWebhook(
                any(),
                anyString()
        );
    }

    private String currentTimestamp() {

        return String.valueOf(
                Instant.now()
                        .getEpochSecond()
        );
    }

    private String sign(
            String secret,
            String timestamp,
            String body
    ) throws Exception {

        Mac mac =
                Mac.getInstance(
                        "HmacSHA256"
                );

        mac.init(
                new SecretKeySpec(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                )
        );

        mac.update(
                timestamp.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        mac.update(
                (byte) '.'
        );

        byte[] signature =
                mac.doFinal(
                        body.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        return "sha256="
                + HexFormat.of()
                .formatHex(
                        signature
                );
    }
}