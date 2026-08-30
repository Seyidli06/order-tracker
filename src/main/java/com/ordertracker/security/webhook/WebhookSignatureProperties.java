package com.ordertracker.security.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(
        prefix = "security.webhook"
)
public class WebhookSignatureProperties {

    private String paymentSecret = "";

    private String shipmentSecret = "";

    private long timestampToleranceSeconds = 300;

    public String getPaymentSecret() {
        return paymentSecret;
    }

    public void setPaymentSecret(
            String paymentSecret
    ) {
        this.paymentSecret = paymentSecret;
    }

    public String getShipmentSecret() {
        return shipmentSecret;
    }

    public void setShipmentSecret(
            String shipmentSecret
    ) {
        this.shipmentSecret = shipmentSecret;
    }

    public long getTimestampToleranceSeconds() {
        return timestampToleranceSeconds;
    }

    public void setTimestampToleranceSeconds(
            long timestampToleranceSeconds
    ) {
        this.timestampToleranceSeconds =
                timestampToleranceSeconds;
    }
}