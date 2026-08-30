package com.ordertracker.notification.integration;

import com.ordertracker.config.AsyncConfig;
import com.ordertracker.notification.dto.EmailNotificationRequest;
import com.ordertracker.notification.service.NotificationService;
import com.ordertracker.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = {
                AsyncConfig.class,
                NotificationService.class
        }
)
class EmailNotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private EmailNotificationRequest emailRequest;

    @BeforeEach
    void setUp() {
        /*
         * Hər test əvvəlki mock interaction və
         * stubbing-lərdən tam təmiz başlayır.
         */
        reset(javaMailSender);

        emailRequest =
                TestDataFactory
                        .createEmailNotificationRequest();
    }

    @Test
    void sendOrderStatusEmail_AsyncExecution_Success() {

        notificationService
                .sendOrderStatusEmail(
                        emailRequest
                );

        awaitEmailCount(1);
    }

    @Test
    void sendOrderStatusEmail_AsyncExecution_MultipleEmails() {

        EmailNotificationRequest request1 =
                TestDataFactory
                        .createEmailNotificationRequest(
                                "user1@example.com",
                                "Order Shipped",
                                "Your order has been shipped"
                        );

        EmailNotificationRequest request2 =
                TestDataFactory
                        .createEmailNotificationRequest(
                                "user2@example.com",
                                "Order Delivered",
                                "Your order has been delivered"
                        );

        notificationService
                .sendOrderStatusEmail(
                        request1
                );

        notificationService
                .sendOrderStatusEmail(
                        request2
                );

        awaitEmailCount(2);
    }

    @Test
    void sendOrderStatusEmail_AsyncExecution_FailureHandling() {

        doThrow(
                new MailSendException(
                        "SMTP server unavailable"
                )
        )
                .when(javaMailSender)
                .send(
                        any(SimpleMailMessage.class)
                );

        notificationService
                .sendOrderStatusEmail(
                        emailRequest
                );

        /*
         * Async task bu test bitməmiş
         * tamamlanmalıdır.
         */
        awaitEmailCount(1);
    }

    @Test
    void sendOrderStatusEmail_AsyncExecution_NonBlocking() {

        long startTime =
                System.currentTimeMillis();

        notificationService
                .sendOrderStatusEmail(
                        emailRequest
                );

        long elapsedTime =
                System.currentTimeMillis()
                        - startTime;

        assertTrue(
                elapsedTime < 1000,
                "Async method should return immediately"
        );

        /*
         * Əvvəlki versiyada test burada bitirdi.
         *
         * Async email isə sonrakı test başlayandan
         * sonra işləyə bilirdi və həmin testdə
         * əlavə üçüncü invocation yaranırdı.
         *
         * İndi async task-ın bu test daxilində
         * tamamlanmasını gözləyirik.
         */
        awaitEmailCount(1);
    }

    @Test
    void sendOrderStatusEmail_AsyncExecution_WithOrderId() {

        EmailNotificationRequest requestWithOrderId =
                EmailNotificationRequest
                        .builder()
                        .recipientEmail(
                                "customer@example.com"
                        )
                        .subject(
                                "Order Status Update"
                        )
                        .body(
                                "Your order status has been updated to SHIPPED"
                        )
                        .orderId(
                                "order_integration_test_123"
                        )
                        .build();

        notificationService
                .sendOrderStatusEmail(
                        requestWithOrderId
                );

        awaitEmailCount(1);
    }

    private void awaitEmailCount(
            int expectedCount
    ) {

        await()
                .atMost(
                        5,
                        TimeUnit.SECONDS
                )
                .untilAsserted(
                        () -> verify(
                                javaMailSender,
                                times(expectedCount)
                        ).send(
                                any(
                                        SimpleMailMessage.class
                                )
                        )
                );
    }
}