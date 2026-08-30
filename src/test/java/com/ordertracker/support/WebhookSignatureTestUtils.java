package com.ordertracker.support;

import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

public final class WebhookSignatureTestUtils {

    public static final String PAYMENT_SECRET =
            "test-payment-webhook-secret";

    public static final String SHIPMENT_SECRET =
            "test-shipment-webhook-secret";

    private static final String TIMESTAMP_HEADER =
            "X-Webhook-Timestamp";

    private static final String SIGNATURE_HEADER =
            "X-Webhook-Signature";

    private WebhookSignatureTestUtils() {
    }

    public static void addPaymentSignature(
            HttpHeaders headers,
            String body
    ) throws Exception {

        addSignature(
                headers,
                body,
                PAYMENT_SECRET
        );
    }

    public static void addShipmentSignature(
            HttpHeaders headers,
            String body
    ) throws Exception {

        addSignature(
                headers,
                body,
                SHIPMENT_SECRET
        );
    }

    private static void addSignature(
            HttpHeaders headers,
            String body,
            String secret
    ) throws Exception {

        String timestamp =
                String.valueOf(
                        Instant.now()
                                .getEpochSecond()
                );

        byte[] bodyBytes =
                body.getBytes(
                        StandardCharsets.UTF_8
                );

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
                        bodyBytes
                );

        headers.set(
                TIMESTAMP_HEADER,
                timestamp
        );

        headers.set(
                SIGNATURE_HEADER,
                "sha256="
                        + HexFormat.of()
                        .formatHex(
                                signature
                        )
        );
    }
}