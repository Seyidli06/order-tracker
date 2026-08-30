package com.ordertracker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.audit.WebhookAuditLogRepository;
import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.Role;
import com.ordertracker.order.entity.Order;
import com.ordertracker.order.repository.OrderRepository;
import com.ordertracker.order.repository.OrderStatusHistoryRepository;
import com.ordertracker.support.TestDataFactory;
import com.ordertracker.user.entity.User;
import com.ordertracker.user.repository.UserRepository;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.ordertracker.support.WebhookSignatureTestUtils;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.health.mail.enabled=false"
)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Testcontainers
class OrderWebhookFlowIntegrationTest {

    private static final String ORDER_ID =
            "order_e2e_test_12345";

    private static final String USER_EMAIL =
            "e2e@example.com";

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    @DynamicPropertySource
    static void setProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgresContainer::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgresContainer::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgresContainer::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                postgresContainer::getDriverClassName
        );
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookAuditLogRepository
            webhookAuditLogRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository
            orderStatusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeAll
    static void beforeAll() {
        postgresContainer.start();
    }

    @AfterAll
    static void afterAll() {
        postgresContainer.stop();
    }

    @BeforeEach
    void setUp() {

        webhookAuditLogRepository.deleteAll();
        orderStatusHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        reset(javaMailSender);

        User user = User.builder()
                .email(USER_EMAIL)
                .password("test-password")
                .role(Role.USER)
                .build();

        user = userRepository.saveAndFlush(user);

        Order order = Order.builder()
                .externalOrderId(ORDER_ID)
                .user(user)
                .totalAmount(
                        new BigDecimal("99.99")
                )
                .currency("USD")
                .status(OrderStatus.CREATED)
                .build();

        orderRepository.saveAndFlush(order);
    }

    @Test
    void endToEndWebhookFlow_PaymentSuccessToShipped_CompleteFlow()
            throws Exception {

        String paymentEventId =
                "pay_e2e_evt_001";

        String shipmentEventId =
                "ship_e2e_evt_001";

        PaymentWebhookPayload paymentPayload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        paymentPayload.setEventId(
                paymentEventId
        );

        paymentPayload
                .getPaymentData()
                .setOrderId(
                        ORDER_ID
                );

        ResponseEntity<String> paymentResponse =
                sendPaymentWebhook(
                        paymentPayload
                );

        assertEquals(
                HttpStatus.OK,
                paymentResponse.getStatusCode()
        );

        awaitAuditStatus(
                paymentEventId,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.PAID
        );

        awaitEmailCount(1);

        ShipmentWebhookPayload shipmentPayload =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "SHIPPED"
                        );

        shipmentPayload.setEventId(
                shipmentEventId
        );

        shipmentPayload
                .getShipmentData()
                .setOrderId(
                        ORDER_ID
                );

        ResponseEntity<String> shipmentResponse =
                sendShipmentWebhook(
                        shipmentPayload
                );

        assertEquals(
                HttpStatus.OK,
                shipmentResponse.getStatusCode()
        );

        awaitAuditStatus(
                shipmentEventId,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.SHIPPED
        );

        awaitEmailCount(2);

        await()
                .atMost(
                        10,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> assertEquals(
                                2,
                                webhookAuditLogRepository
                                        .count()
                        )
                );
    }

    @Test
    void endToEndWebhookFlow_PaymentFailedToShipped_CompleteFlow()
            throws Exception {

        String failedPaymentEventId =
                "pay_e2e_evt_002";

        String successfulPaymentEventId =
                "pay_e2e_evt_002_retry";

        String shipmentEventId =
                "ship_e2e_evt_002";

        /*
         * CREATED -> PAYMENT_FAILED
         */
        PaymentWebhookPayload failedPaymentPayload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_FAILED"
                        );

        failedPaymentPayload.setEventId(
                failedPaymentEventId
        );

        failedPaymentPayload
                .getPaymentData()
                .setOrderId(
                        ORDER_ID
                );

        ResponseEntity<String> failedPaymentResponse =
                sendPaymentWebhook(
                        failedPaymentPayload
                );

        assertEquals(
                HttpStatus.OK,
                failedPaymentResponse.getStatusCode()
        );

        awaitAuditStatus(
                failedPaymentEventId,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.PAYMENT_FAILED
        );

        awaitEmailCount(1);

        /*
         * PAYMENT_FAILED -> PAID
         *
         * Failed payment-dən sonra shipment göndərmək
         * düzgün business flow deyil.
         *
         * Əvvəl payment retry successful olmalıdır.
         */
        PaymentWebhookPayload successfulPaymentPayload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        successfulPaymentPayload.setEventId(
                successfulPaymentEventId
        );

        successfulPaymentPayload
                .getPaymentData()
                .setOrderId(
                        ORDER_ID
                );

        ResponseEntity<String> successfulPaymentResponse =
                sendPaymentWebhook(
                        successfulPaymentPayload
                );

        assertEquals(
                HttpStatus.OK,
                successfulPaymentResponse.getStatusCode()
        );

        awaitAuditStatus(
                successfulPaymentEventId,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.PAID
        );

        awaitEmailCount(2);

        /*
         * PAID -> SHIPPED
         */
        ShipmentWebhookPayload shipmentPayload =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "SHIPPED"
                        );

        shipmentPayload.setEventId(
                shipmentEventId
        );

        shipmentPayload
                .getShipmentData()
                .setOrderId(
                        ORDER_ID
                );

        ResponseEntity<String> shipmentResponse =
                sendShipmentWebhook(
                        shipmentPayload
                );

        assertEquals(
                HttpStatus.OK,
                shipmentResponse.getStatusCode()
        );

        awaitAuditStatus(
                shipmentEventId,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.SHIPPED
        );

        awaitEmailCount(3);
    }

    @Test
    void endToEndWebhookFlow_DuplicatePaymentEvent_PreventsDuplicateProcessing()
            throws Exception {

        String paymentEventId =
                "pay_e2e_evt_003";

        PaymentWebhookPayload paymentPayload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        paymentPayload.setEventId(
                paymentEventId
        );

        paymentPayload
                .getPaymentData()
                .setOrderId(
                        ORDER_ID
                );

        ResponseEntity<String> firstResponse =
                sendPaymentWebhook(
                        paymentPayload
                );

        assertEquals(
                HttpStatus.OK,
                firstResponse.getStatusCode()
        );

        awaitAuditStatus(
                paymentEventId,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.PAID
        );

        awaitEmailCount(1);

        long initialAuditCount =
                webhookAuditLogRepository
                        .count();

        ResponseEntity<String> duplicateResponse =
                sendPaymentWebhook(
                        paymentPayload
                );

        assertEquals(
                HttpStatus.OK,
                duplicateResponse.getStatusCode()
        );

        /*
         * Duplicate event yeni audit record
         * yaratmamalıdır.
         */
        await()
                .pollDelay(
                        500,
                        TimeUnit.MILLISECONDS
                )
                .atMost(
                        3,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> assertEquals(
                                initialAuditCount,
                                webhookAuditLogRepository
                                        .count()
                        )
                );

        /*
         * Duplicate event ikinci notification
         * göndərməməlidir.
         */
        await()
                .pollDelay(
                        500,
                        TimeUnit.MILLISECONDS
                )
                .atMost(
                        3,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> verify(
                                javaMailSender,
                                times(1)
                        ).send(
                                any(
                                        SimpleMailMessage.class
                                )
                        )
                );

        awaitOrderStatus(
                OrderStatus.PAID
        );
    }

    @Test
    void endToEndWebhookFlow_MultipleShipmentsForSameOrder_ProcessesAll()
            throws Exception {

        /*
         * Shipment testinin məqsədi payment flow deyil.
         * Ona görə fixture order-i əvvəlcədən
         * PAID vəziyyətinə gətiririk.
         */
        setOrderStatus(
                OrderStatus.PAID
        );

        String shipmentEventId1 =
                "ship_e2e_evt_004";

        String shipmentEventId2 =
                "ship_e2e_evt_005";

        ShipmentWebhookPayload shipmentPayload1 =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "SHIPPED"
                        );

        shipmentPayload1.setEventId(
                shipmentEventId1
        );

        shipmentPayload1
                .getShipmentData()
                .setOrderId(
                        ORDER_ID
                );

        /*
         * PAID -> SHIPPED
         */
        ResponseEntity<String> firstResponse =
                sendShipmentWebhook(
                        shipmentPayload1
                );

        assertEquals(
                HttpStatus.OK,
                firstResponse.getStatusCode()
        );

        /*
         * İkinci webhook-u birinci async processing
         * bitmədən göndərmirik.
         *
         * Yoxsa OUT_FOR_DELIVERY webhook-u order hələ
         * PAID ikən işlənə bilər.
         */
        awaitAuditStatus(
                shipmentEventId1,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.SHIPPED
        );

        awaitEmailCount(1);

        ShipmentWebhookPayload shipmentPayload2 =
                TestDataFactory
                        .createShipmentWebhookPayload(
                                "OUT_FOR_DELIVERY"
                        );

        shipmentPayload2.setEventId(
                shipmentEventId2
        );

        shipmentPayload2
                .getShipmentData()
                .setOrderId(
                        ORDER_ID
                );

        /*
         * SHIPPED -> OUT_FOR_DELIVERY
         */
        ResponseEntity<String> secondResponse =
                sendShipmentWebhook(
                        shipmentPayload2
                );

        assertEquals(
                HttpStatus.OK,
                secondResponse.getStatusCode()
        );

        awaitAuditStatus(
                shipmentEventId2,
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );

        awaitOrderStatus(
                OrderStatus.OUT_FOR_DELIVERY
        );

        awaitEmailCount(2);

        assertEquals(
                2,
                webhookAuditLogRepository
                        .count()
        );
    }

    private ResponseEntity<String> sendPaymentWebhook(
            PaymentWebhookPayload payload
    ) throws Exception {

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        HttpHeaders headers =
                createJsonHeaders();

        WebhookSignatureTestUtils
                .addPaymentSignature(
                        headers,
                        body
                );

        HttpEntity<String> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        return restTemplate.postForEntity(
                "http://localhost:"
                        + port
                        + "/api/webhooks/payment",
                request,
                String.class
        );
    }

    private ResponseEntity<String> sendShipmentWebhook(
            ShipmentWebhookPayload payload
    ) throws Exception {

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        HttpHeaders headers =
                createJsonHeaders();

        WebhookSignatureTestUtils
                .addShipmentSignature(
                        headers,
                        body
                );

        HttpEntity<String> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        return restTemplate.postForEntity(
                "http://localhost:"
                        + port
                        + "/api/webhooks/shipment",
                request,
                String.class
        );
    }

    private HttpHeaders createJsonHeaders() {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        return headers;
    }

    private void awaitAuditStatus(
            String eventId,
            WebhookAuditLog.ProcessingStatus expectedStatus
    ) {

        await()
                .atMost(
                        10,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> {

                            WebhookAuditLog auditLog =
                                    webhookAuditLogRepository
                                            .findByEventId(
                                                    eventId
                                            )
                                            .orElse(
                                                    null
                                            );

                            assertNotNull(
                                    auditLog,
                                    "Webhook audit log should exist: "
                                            + eventId
                            );

                            assertEquals(
                                    expectedStatus,
                                    auditLog
                                            .getProcessingStatus()
                            );
                        }
                );
    }

    private void awaitOrderStatus(
            OrderStatus expectedStatus
    ) {

        await()
                .atMost(
                        10,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> {

                            Order order =
                                    orderRepository
                                            .findByExternalOrderId(
                                                    ORDER_ID
                                            )
                                            .orElseThrow();

                            assertEquals(
                                    expectedStatus,
                                    order.getStatus()
                            );
                        }
                );
    }

    private void awaitEmailCount(
            int expectedCount
    ) {

        await()
                .atMost(
                        10,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> verify(
                                javaMailSender,
                                times(
                                        expectedCount
                                )
                        ).send(
                                any(
                                        SimpleMailMessage.class
                                )
                        )
                );
    }

    private void setOrderStatus(
            OrderStatus status
    ) {

        Order order =
                orderRepository
                        .findByExternalOrderId(
                                ORDER_ID
                        )
                        .orElseThrow();

        order.setStatus(
                status
        );

        orderRepository.saveAndFlush(
                order
        );
    }


}