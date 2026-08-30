package com.ordertracker.security.webhook;

import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookSignatureFilterTest {

    @Mock
    private WebhookSignatureVerifier
            signatureVerifier;

    @Mock
    private SecurityErrorResponseWriter
            securityErrorResponseWriter;

    @Mock
    private FilterChain filterChain;

    private WebhookSignatureFilter filter;

    @BeforeEach
    void setUp() {

        filter =
                new WebhookSignatureFilter(
                        signatureVerifier,
                        securityErrorResponseWriter
                );
    }

    @Test
    void validPaymentSignature_ShouldContinueFilterChain()
            throws Exception {

        byte[] body =
                """
                {"event_id":"evt_123"}
                """
                        .trim()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        MockHttpServletRequest request =
                request(
                        "/api/webhooks/payment",
                        body
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(
                signatureVerifier.isValid(
                        eq(WebhookType.PAYMENT),
                        eq("1234567890"),
                        eq("sha256=test"),
                        any(byte[].class)
                )
        ).thenReturn(true);

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                any(CachedBodyHttpServletRequest.class),
                eq(response)
        );

        verify(
                securityErrorResponseWriter,
                never()
        ).write(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void invalidPaymentSignature_ShouldReturnUnauthorized()
            throws Exception {

        byte[] body =
                "{}".getBytes(
                        StandardCharsets.UTF_8
                );

        MockHttpServletRequest request =
                request(
                        "/api/webhooks/payment",
                        body
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(
                signatureVerifier.isValid(
                        eq(WebhookType.PAYMENT),
                        eq("1234567890"),
                        eq("sha256=test"),
                        any(byte[].class)
                )
        ).thenReturn(false);

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                securityErrorResponseWriter
        ).write(
                eq(request),
                eq(response),
                eq(HttpStatus.UNAUTHORIZED),
                eq("Invalid webhook signature")
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                any(),
                any()
        );
    }

    @Test
    void validShipmentSignature_ShouldUseShipmentSecret()
            throws Exception {

        MockHttpServletRequest request =
                request(
                        "/api/webhooks/shipment",
                        "{}".getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(
                signatureVerifier.isValid(
                        eq(WebhookType.SHIPMENT),
                        eq("1234567890"),
                        eq("sha256=test"),
                        any(byte[].class)
                )
        ).thenReturn(true);

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                signatureVerifier
        ).isValid(
                eq(WebhookType.SHIPMENT),
                eq("1234567890"),
                eq("sha256=test"),
                any(byte[].class)
        );

        verify(
                filterChain
        ).doFilter(
                any(CachedBodyHttpServletRequest.class),
                eq(response)
        );
    }

    @Test
    void missingSignature_ShouldRejectRequest()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("POST");
        request.setRequestURI(
                "/api/webhooks/payment"
        );

        request.addHeader(
                WebhookSignatureFilter.TIMESTAMP_HEADER,
                "1234567890"
        );

        request.setContent(
                "{}".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(
                signatureVerifier.isValid(
                        eq(WebhookType.PAYMENT),
                        eq("1234567890"),
                        eq(null),
                        any(byte[].class)
                )
        ).thenReturn(false);

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                securityErrorResponseWriter
        ).write(
                eq(request),
                eq(response),
                eq(HttpStatus.UNAUTHORIZED),
                eq("Invalid webhook signature")
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                any(),
                any()
        );
    }

    @Test
    void nonWebhookEndpoint_ShouldSkipFilter()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI(
                "/api/orders"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );

        verify(
                signatureVerifier,
                never()
        ).isValid(
                any(),
                any(),
                any(),
                any()
        );
    }

    private MockHttpServletRequest request(
            String path,
            byte[] body
    ) {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("POST");
        request.setRequestURI(path);

        request.setContent(body);

        request.addHeader(
                WebhookSignatureFilter.TIMESTAMP_HEADER,
                "1234567890"
        );

        request.addHeader(
                WebhookSignatureFilter.SIGNATURE_HEADER,
                "sha256=test"
        );

        return request;
    }
}