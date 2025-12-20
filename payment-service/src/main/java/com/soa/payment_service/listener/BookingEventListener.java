package com.soa.payment_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingFailedEvent;
import com.soa.common.event.BookingReadyForPaymentEvent;
import com.soa.payment_service.config.RabbitMQConfig;
import com.soa.payment_service.service.PaymentService;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_READY_FOR_PAYMENT)
    public void handleBookingReadyForPayment(BookingReadyForPaymentEvent event) {
        log.info("Received booking.ready_for_payment event: {}", event);
        paymentService.createAndConfirmPayment(event);
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_FAILED)
    public void handleBookingFailed(BookingFailedEvent event) {
        log.info("Received booking.failed event: {}", event);
        try {
            paymentService.cancelPaymentForBooking(event.getBookingId());
        } catch (Exception e) {
            log.error("Error processing booking failed event", e);
        }
    }
}