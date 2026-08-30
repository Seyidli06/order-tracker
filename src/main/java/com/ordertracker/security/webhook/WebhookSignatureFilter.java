package com.ordertracker.security.webhook;

import com.ordertracker.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureFilter
        extends OncePerRequestFilter {

    public static final String TIMESTAMP_HEADER =
            "X-Webhook-Timestamp";

    public static final String SIGNATURE_HEADER =
            "X-Webhook-Signature";

    private static final String PAYMENT_PATH =
            "/api/webhooks/payment";

    private static final String SHIPMENT_PATH =
            "/api/webhooks/shipment";

    private final WebhookSignatureVerifier
            signatureVerifier;

    private final SecurityErrorResponseWriter
            securityErrorResponseWriter;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        if (!"POST".equalsIgnoreCase(
                request.getMethod()
        )) {
            return true;
        }

        String path =
                request.getRequestURI();

        return !PAYMENT_PATH.equals(path)
                && !SHIPMENT_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        CachedBodyHttpServletRequest wrappedRequest =
                new CachedBodyHttpServletRequest(
                        request
                );

        WebhookType webhookType =
                resolveWebhookType(
                        request.getRequestURI()
                );

        String timestamp =
                request.getHeader(
                        TIMESTAMP_HEADER
                );

        String signature =
                request.getHeader(
                        SIGNATURE_HEADER
                );

        boolean valid =
                signatureVerifier.isValid(
                        webhookType,
                        timestamp,
                        signature,
                        wrappedRequest.getCachedBody()
                );

        if (!valid) {

            log.warn(
                    "Rejected webhook with invalid signature: type={}, path={}, remoteAddress={}",
                    webhookType,
                    request.getRequestURI(),
                    request.getRemoteAddr()
            );

            securityErrorResponseWriter.write(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid webhook signature"
            );

            return;
        }

        filterChain.doFilter(
                wrappedRequest,
                response
        );
    }

    private WebhookType resolveWebhookType(
            String path
    ) {

        if (PAYMENT_PATH.equals(path)) {
            return WebhookType.PAYMENT;
        }

        if (SHIPMENT_PATH.equals(path)) {
            return WebhookType.SHIPMENT;
        }

        throw new IllegalArgumentException(
                "Unsupported webhook path: "
                        + path
        );
    }
}