package com.ordertracker.security.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM =
            "HmacSHA256";

    private static final String SIGNATURE_PREFIX =
            "sha256=";

    private final WebhookSignatureProperties
            properties;

    public boolean isValid(
            WebhookType webhookType,
            String timestampHeader,
            String signatureHeader,
            byte[] rawBody
    ) {

        if (webhookType == null
                || timestampHeader == null
                || timestampHeader.isBlank()
                || signatureHeader == null
                || signatureHeader.isBlank()
                || rawBody == null) {

            return false;
        }

        String secret =
                resolveSecret(
                        webhookType
                );

        if (secret == null
                || secret.isBlank()) {

            log.error(
                    "Webhook secret is not configured for type={}",
                    webhookType
            );

            return false;
        }

        long timestamp;

        try {
            timestamp =
                    Long.parseLong(
                            timestampHeader
                    );
        } catch (NumberFormatException e) {

            return false;
        }

        if (!isTimestampValid(timestamp)) {
            return false;
        }

        byte[] suppliedSignature =
                decodeSignature(
                        signatureHeader
                );

        if (suppliedSignature == null) {
            return false;
        }

        byte[] expectedSignature =
                calculateSignature(
                        secret,
                        timestampHeader,
                        rawBody
                );

        return MessageDigest.isEqual(
                expectedSignature,
                suppliedSignature
        );
    }

    private boolean isTimestampValid(
            long timestamp
    ) {

        long now =
                Instant.now()
                        .getEpochSecond();

        long tolerance =
                properties
                        .getTimestampToleranceSeconds();

        if (tolerance < 0) {
            return false;
        }

        return timestamp >= now - tolerance
                && timestamp <= now + tolerance;
    }

    private String resolveSecret(
            WebhookType webhookType
    ) {

        return switch (webhookType) {

            case PAYMENT ->
                    properties.getPaymentSecret();

            case SHIPMENT ->
                    properties.getShipmentSecret();
        };
    }

    private byte[] calculateSignature(
            String secret,
            String timestamp,
            byte[] rawBody
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            HMAC_ALGORITHM
                    );

            SecretKeySpec key =
                    new SecretKeySpec(
                            secret.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            HMAC_ALGORITHM
                    );

            mac.init(key);

            mac.update(
                    timestamp.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            mac.update(
                    (byte) '.'
            );

            return mac.doFinal(
                    rawBody
            );

        } catch (GeneralSecurityException e) {

            throw new IllegalStateException(
                    "Unable to calculate webhook signature",
                    e
            );
        }
    }

    private byte[] decodeSignature(
            String signatureHeader
    ) {

        String signature =
                signatureHeader.trim();

        if (signature.startsWith(
                SIGNATURE_PREFIX
        )) {

            signature =
                    signature.substring(
                            SIGNATURE_PREFIX.length()
                    );
        }

        try {

            return HexFormat
                    .of()
                    .parseHex(
                            signature
                    );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }
}