package com.ordertracker.security.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureVerifierTest {

    private static final String PAYMENT_SECRET =
            "test-payment-secret";

    private static final String SHIPMENT_SECRET =
            "test-shipment-secret";

    private WebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {

        WebhookSignatureProperties properties =
                new WebhookSignatureProperties();

        properties.setPaymentSecret(
                PAYMENT_SECRET
        );

        properties.setShipmentSecret(
                SHIPMENT_SECRET
        );

        properties.setTimestampToleranceSeconds(
                300
        );

        verifier =
                new WebhookSignatureVerifier(
                        properties
                );
    }

    @Test
    void validPaymentSignature_ShouldPass()
            throws Exception {

        byte[] body =
                """
                {"event_id":"evt_123"}
                """
                        .trim()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .getEpochSecond()
                );

        String signature =
                sign(
                        PAYMENT_SECRET,
                        timestamp,
                        body
                );

        assertTrue(
                verifier.isValid(
                        WebhookType.PAYMENT,
                        timestamp,
                        signature,
                        body
                )
        );
    }

    @Test
    void validShipmentSignature_ShouldPass()
            throws Exception {

        byte[] body =
                """
                {"event_id":"ship_123"}
                """
                        .trim()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .getEpochSecond()
                );

        String signature =
                sign(
                        SHIPMENT_SECRET,
                        timestamp,
                        body
                );

        assertTrue(
                verifier.isValid(
                        WebhookType.SHIPMENT,
                        timestamp,
                        signature,
                        body
                )
        );
    }

    @Test
    void wrongSignature_ShouldFail() {

        byte[] body =
                "{}".getBytes(
                        StandardCharsets.UTF_8
                );

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .getEpochSecond()
                );

        assertFalse(
                verifier.isValid(
                        WebhookType.PAYMENT,
                        timestamp,
                        "sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        body
                )
        );
    }

    @Test
    void staleTimestamp_ShouldFail()
            throws Exception {

        byte[] body =
                "{}".getBytes(
                        StandardCharsets.UTF_8
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

        assertFalse(
                verifier.isValid(
                        WebhookType.PAYMENT,
                        timestamp,
                        signature,
                        body
                )
        );
    }

    @Test
    void missingSignature_ShouldFail() {

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .getEpochSecond()
                );

        assertFalse(
                verifier.isValid(
                        WebhookType.PAYMENT,
                        timestamp,
                        null,
                        "{}".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );
    }

    @Test
    void paymentSignature_ShouldNotWorkForShipment()
            throws Exception {

        byte[] body =
                "{}".getBytes(
                        StandardCharsets.UTF_8
                );

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .getEpochSecond()
                );

        String paymentSignature =
                sign(
                        PAYMENT_SECRET,
                        timestamp,
                        body
                );

        assertFalse(
                verifier.isValid(
                        WebhookType.SHIPMENT,
                        timestamp,
                        paymentSignature,
                        body
                )
        );
    }

    private String sign(
            String secret,
            String timestamp,
            byte[] body
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
                mac.doFinal(body);

        return "sha256="
                + HexFormat.of()
                .formatHex(
                        signature
                );
    }
}