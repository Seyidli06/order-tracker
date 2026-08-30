package com.ordertracker.webhook.integration;

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
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Testcontainers
class PaymentWebhookIntegrationTest {

    private static final String ORDER_ID =
            "order_test_12345";

    @Container
    static final PostgreSQLContainer<?> postgresContainer =
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

    @BeforeEach
    void setUp() {

        webhookAuditLogRepository.deleteAll();
        orderStatusHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        reset(javaMailSender);

        User user = User.builder()
                .email("payment-test@example.com")
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
    void handlePaymentWebhook_ValidPayload_FullFlowWorks()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        ResponseEntity<String> response =
                sendWebhook(payload);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        awaitProcessed(
                payload.getEventId()
        );

        awaitOrderStatus(
                OrderStatus.PAID
        );

        awaitEmailCount(1);
    }

    @Test
    void handlePaymentWebhook_PaymentFailed_FullFlowWorks()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_FAILED"
                        );

        ResponseEntity<String> response =
                sendWebhook(payload);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        awaitProcessed(
                payload.getEventId()
        );

        awaitOrderStatus(
                OrderStatus.PAYMENT_FAILED
        );

        awaitEmailCount(1);
    }

    @Test
    void handlePaymentWebhook_DuplicateEvent_ShouldNotCreateDuplicate()
            throws Exception {

        PaymentWebhookPayload payload =
                TestDataFactory
                        .createPaymentWebhookPayload(
                                "PAYMENT_SUCCEEDED"
                        );

        ResponseEntity<String> firstResponse =
                sendWebhook(payload);

        assertEquals(
                HttpStatus.OK,
                firstResponse.getStatusCode()
        );

        awaitProcessed(
                payload.getEventId()
        );

        awaitOrderStatus(
                OrderStatus.PAID
        );

        awaitEmailCount(1);

        long initialCount =
                webhookAuditLogRepository.count();

        ResponseEntity<String> secondResponse =
                sendWebhook(payload);

        assertEquals(
                HttpStatus.OK,
                secondResponse.getStatusCode()
        );

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
                                initialCount,
                                webhookAuditLogRepository
                                        .count()
                        )
                );

        verify(
                javaMailSender,
                times(1)
        ).send(
                any(SimpleMailMessage.class)
        );
    }

    private ResponseEntity<String> sendWebhook(
            PaymentWebhookPayload payload
    ) throws Exception {

        String body =
                objectMapper.writeValueAsString(
                        payload
                );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

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

    private void awaitProcessed(
            String eventId
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
                                            .orElse(null);

                            assertNotNull(
                                    auditLog
                            );

                            assertEquals(
                                    WebhookAuditLog
                                            .ProcessingStatus
                                            .PROCESSED,
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
                                times(expectedCount)
                        ).send(
                                any(SimpleMailMessage.class)
                        )
                );
    }
}