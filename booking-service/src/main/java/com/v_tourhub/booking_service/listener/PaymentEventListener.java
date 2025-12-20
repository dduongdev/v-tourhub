package com.v_tourhub.booking_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.soa.common.event.PaymentCompletedEvent;
import com.soa.common.event.PaymentFailedEvent;
import com.v_tourhub.booking_service.config.RabbitMQConfig;
import com.v_tourhub.booking_service.service.BookingService;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingService bookingService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_COMPLETED)
    public void handlePaymentCompleted(PaymentCompletedEvent event) { 
        log.info("Received PaymentCompletedEvent: {}", event);
        try {
            bookingService.completeBooking(event.getBookingId(), event.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing payment completed event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_FAILED)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: {}", event);
        try {
            bookingService.handlePaymentFailure(event.getBookingId(), event.getReason());
        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }

}