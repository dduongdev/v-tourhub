package com.v_tourhub.notification_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.soa.common.event.BookingConfirmedEvent;
import com.v_tourhub.notification_service.config.RabbitMQConfig;
import com.v_tourhub.notification_service.service.EmailService;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
    public void handleBookingConfirmed(BookingConfirmedEvent event) { 
        log.info("Received BookingConfirmedEvent: {}", event);
        
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().isEmpty()) {
            emailService.sendBookingConfirmation(event);
        } else {
            log.warn("No customer email found in event, skipping email sending.");
        }
    }
}
