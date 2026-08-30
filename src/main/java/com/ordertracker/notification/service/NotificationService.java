package com.ordertracker.notification.service;

import com.ordertracker.notification.dto.EmailNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOrderStatusEmail(
            EmailNotificationRequest request
    ) {

        if (request == null) {
            log.warn(
                    "Skipping email notification because request is null"
            );
            return;
        }

        if (request.getRecipientEmail() == null
                || request.getRecipientEmail().isBlank()) {

            log.warn(
                    "Skipping email notification because recipient is missing, orderId={}",
                    request.getOrderId()
            );
            return;
        }

        if (request.getSubject() == null
                || request.getSubject().isBlank()) {

            log.warn(
                    "Skipping email notification because subject is missing, recipient={}, orderId={}",
                    request.getRecipientEmail(),
                    request.getOrderId()
            );
            return;
        }

        if (request.getBody() == null
                || request.getBody().isBlank()) {

            log.warn(
                    "Skipping email notification because body is missing, recipient={}, orderId={}",
                    request.getRecipientEmail(),
                    request.getOrderId()
            );
            return;
        }

        try {
            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(
                    request.getRecipientEmail()
            );

            message.setSubject(
                    request.getSubject()
            );

            message.setText(
                    request.getBody()
            );

            mailSender.send(message);

            log.info(
                    "Email sent successfully to: {}, orderId: {}",
                    request.getRecipientEmail(),
                    request.getOrderId()
            );

        } catch (MailException e) {

            log.error(
                    "Failed to send email to: {}, orderId: {}, error: {}",
                    request.getRecipientEmail(),
                    request.getOrderId(),
                    e.getMessage(),
                    e
            );

        } catch (Exception e) {

            log.error(
                    "Unexpected error while sending email to: {}, orderId: {}",
                    request.getRecipientEmail(),
                    request.getOrderId(),
                    e
            );
        }
    }
}