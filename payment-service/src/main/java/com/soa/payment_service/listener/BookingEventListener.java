package com.soa.payment_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingCreatedEvent;
import com.soa.payment_service.config.RabbitMQConfig;
import com.soa.payment_service.service.PaymentService;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_PROCESS)
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Received BookingCreatedEvent: {}", event);
        try {
            paymentService.initPayment(event.getBookingId(), event.getUserId(), event.getAmount());
        } catch (Exception e) {
            log.error("Error processing booking event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CANCELLED)
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Received booking.cancelled event: {}", event);
        try {
            paymentService.handleBookingCancellation(event);
        } catch (Exception e) {
            log.error("Error processing booking cancellation event", e);
        }
    }
}