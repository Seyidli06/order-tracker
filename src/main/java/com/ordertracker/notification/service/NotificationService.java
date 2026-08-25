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
    public void sendOrderStatusEmail(EmailNotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getRecipientEmail());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());

            mailSender.send(message);

            log.info("Email sent successfully to: {}, orderId: {}", request.getRecipientEmail(), request.getOrderId());

        } catch (MailException e) {
            log.error("Failed to send email to: {}, orderId: {}, error: {}",
                    request.getRecipientEmail(), request.getOrderId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to: {}, orderId: {}",
                    request.getRecipientEmail(), request.getOrderId(), e);
        }
    }
}
