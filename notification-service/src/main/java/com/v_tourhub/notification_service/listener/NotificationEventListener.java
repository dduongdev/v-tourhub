package com.v_tourhub.notification_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.v_tourhub.notification_service.config.RabbitMQConfig;
import com.v_tourhub.notification_service.service.EmailService;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
    public void handleBookingConfirmed(Map<String, Object> event) {
        log.info("Received booking confirmed event: {}", event);
        
        String email = (String) event.get("customerEmail");
        if (email != null && !email.isEmpty()) {
            emailService.sendBookingConfirmation(email, event);
        } else {
            log.warn("No customer email found in event, skipping email sending.");
        }
    }
}
