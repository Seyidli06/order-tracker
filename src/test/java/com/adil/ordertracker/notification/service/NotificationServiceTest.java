package com.adil.ordertracker.notification.service;

import com.adil.ordertracker.support.TestDataFactory;
import com.ordertracker.notification.dto.EmailNotificationRequest;
import com.ordertracker.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private NotificationService notificationService;

    private EmailNotificationRequest emailRequest;

    @BeforeEach
    void setUp() {
        emailRequest = TestDataFactory.createEmailNotificationRequest();
    }

    @Test
    void sendOrderStatusEmail_Success() {
        notificationService.sendOrderStatusEmail(emailRequest);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals(emailRequest.getRecipientEmail(), sentMessage.getTo()[0]);
        assertEquals(emailRequest.getSubject(), sentMessage.getSubject());
        assertEquals(emailRequest.getBody(), sentMessage.getText());
    }

    @Test
    void sendOrderStatusEmail_MailException_HandledGracefully() {
        MailException mailException = new MailSendException("SMTP server unavailable");
        doThrow(mailException).when(javaMailSender).send(any(SimpleMailMessage.class));

        notificationService.sendOrderStatusEmail(emailRequest);

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderStatusEmail_GenericException_HandledGracefully() {
        RuntimeException runtimeException = new RuntimeException("Unexpected error");
        doThrow(runtimeException).when(javaMailSender).send(any(SimpleMailMessage.class));

        notificationService.sendOrderStatusEmail(emailRequest);

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderStatusEmail_WithCustomParameters() {
        EmailNotificationRequest customRequest = TestDataFactory.createEmailNotificationRequest(
                "custom@example.com",
                "Custom Subject",
                "Custom Body"
        );

        notificationService.sendOrderStatusEmail(customRequest);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("custom@example.com", sentMessage.getTo()[0]);
        assertEquals("Custom Subject", sentMessage.getSubject());
        assertEquals("Custom Body", sentMessage.getText());
    }

    @Test
    void sendOrderStatusEmail_NullRecipient_DoesNotSend() {
        EmailNotificationRequest invalidRequest = EmailNotificationRequest.builder()
                .recipientEmail(null)
                .subject("Test Subject")
                .body("Test Body")
                .orderId("order_123")
                .build();

        notificationService.sendOrderStatusEmail(invalidRequest);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
}
