package com.ordertracker.webhook.controller;

import com.ordertracker.security.webhook.WebhookSignatureFilter;
import com.ordertracker.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.webhook.controller.WebhookController;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ordertracker.security.jwt.JwtAuthenticationFilter;
import com.ordertracker.security.ratelimit.RateLimitFilter;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.task.TaskRejectedException;

import static org.mockito.Mockito.doThrow;
@WebMvcTest(
        controllers = WebhookController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JwtAuthenticationFilter.class
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = RateLimitFilter.class
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebhookSignatureFilter.class
                )
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ShipmentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.ordertracker.webhook.service.WebhookService webhookService;

    @Test
    void handleShipmentWebhook_ValidPayload_Returns200() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("SHIPPED");

        doNothing().when(webhookService).processShipmentWebhook(any(ShipmentWebhookPayload.class), anyString());

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(webhookService).processShipmentWebhook(any(ShipmentWebhookPayload.class), anyString());
    }

    @Test
    void handleShipmentWebhook_InvalidPayload_MissingEventId_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_type": "shipment.shipped",
                    "source": "fedex",
                    "timestamp": "2026-08-25T09:00:00Z",
                    "shipment_data": {
                        "shipment_id": "shp_test",
                        "order_id": "order_123",
                        "tracking_number": "1234567890123456",
                        "carrier": "fedex",
                        "status": "SHIPPED",
                        "packages": []
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleShipmentWebhook_InvalidPayload_MissingShipmentData_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "ship_evt_123",
                    "event_type": "shipment.shipped",
                    "source": "fedex",
                    "timestamp": "2026-08-25T09:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleShipmentWebhook_InvalidPayload_MissingTrackingNumber_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "ship_evt_123",
                    "event_type": "shipment.shipped",
                    "source": "fedex",
                    "timestamp": "2026-08-25T09:00:00Z",
                    "shipment_data": {
                        "shipment_id": "shp_test",
                        "order_id": "order_123",
                        "carrier": "fedex",
                        "status": "SHIPPED",
                        "packages": []
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleShipmentWebhook_InvalidPayload_MissingPackages_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "ship_evt_123",
                    "event_type": "shipment.shipped",
                    "source": "fedex",
                    "timestamp": "2026-08-25T09:00:00Z",
                    "shipment_data": {
                        "shipment_id": "shp_test",
                        "order_id": "order_123",
                        "tracking_number": "1234567890123456",
                        "carrier": "fedex",
                        "status": "SHIPPED"
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleShipmentWebhook_InvalidPayload_NegativeWeight_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "ship_evt_123",
                    "event_type": "shipment.shipped",
                    "source": "fedex",
                    "timestamp": "2026-08-25T09:00:00Z",
                    "shipment_data": {
                        "shipment_id": "shp_test",
                        "order_id": "order_123",
                        "tracking_number": "1234567890123456",
                        "carrier": "fedex",
                        "status": "SHIPPED",
                        "packages": [
                            {
                                "package_id": "pkg_test",
                                "weight": -2.5,
                                "weightUnit": "kg"
                            }
                        ]
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleShipmentWebhook_ValidPayload_Delivered_Returns200() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("DELIVERED");

        doNothing().when(webhookService).processShipmentWebhook(any(ShipmentWebhookPayload.class), anyString());

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(webhookService).processShipmentWebhook(any(ShipmentWebhookPayload.class), anyString());
    }

    @Test
    void handleShipmentWebhook_ValidPayload_OutForDelivery_Returns200() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("OUT_FOR_DELIVERY");

        doNothing().when(webhookService).processShipmentWebhook(any(ShipmentWebhookPayload.class), anyString());

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(webhookService).processShipmentWebhook(any(ShipmentWebhookPayload.class), anyString());
    }

    @Test
    void handleShipmentWebhook_DispatchFailure_Returns500()
            throws Exception {

        ShipmentWebhookPayload payload =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "SHIPPED"
                        );

        doThrow(
                new TaskRejectedException(
                        "Executor rejected task"
                )
        )
                .when(webhookService)
                .processShipmentWebhook(
                        any(ShipmentWebhookPayload.class),
                        anyString()
                );

        mockMvc.perform(
                        post("/api/webhooks/shipment")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                payload
                                        )
                                )
                )
                .andExpect(
                        status().isInternalServerError()
                );
    }
}
