package com.v_tourhub.notification_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingConfirmedEvent;
import com.soa.common.event.BookingFailedEvent;
import com.soa.common.event.BookingReadyForPaymentEvent;
import com.v_tourhub.notification_service.config.RabbitMQConfig;
import com.v_tourhub.notification_service.service.EmailService;

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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CANCELLATION_EMAIL)
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Received booking cancelled event: {}", event);
        if (event.getCustomerEmail() != null) {
            emailService.sendBookingCancellationEmail(event);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_FAILED_EMAIL)
    public void handleBookingFailed(BookingFailedEvent event) {
        log.info("Received booking.failed event for notification: {}", event);
        
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().isEmpty()) {
            emailService.sendBookingFailureEmail(event);
        } else {
            log.warn("No customer email in booking.failed event, skipping email sending.");
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_READY_FOR_PAYMENT_EMAIL)
    public void handleBookingReadyForPayment(BookingReadyForPaymentEvent event) {
        log.info("Received booking.ready_for_payment event for notification: {}", event);
        emailService.sendPaymentReadyEmail(event);
    }
}
