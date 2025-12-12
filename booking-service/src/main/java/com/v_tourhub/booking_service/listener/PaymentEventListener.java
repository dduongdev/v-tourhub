package com.v_tourhub.booking_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.v_tourhub.booking_service.config.RabbitMQConfig;
import com.v_tourhub.booking_service.service.BookingService;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingService bookingService;

    // --- CASE 1: THANH TOÁN THÀNH CÔNG ---
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_COMPLETED)
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("Received Payment Completed Event: {}", event);
        try {
            Long bookingId = Long.valueOf(event.get("bookingId").toString());
            String transactionId = (String) event.get("transactionId");

            bookingService.completeBooking(bookingId, transactionId);

        } catch (Exception e) {
            log.error("Error processing payment completed event", e);
        }
    }

    // --- CASE 2: THANH TOÁN THẤT BẠI ---
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_FAILED)
    public void handlePaymentFailed(Map<String, Object> event) {
        log.info("Received Payment Failed Event: {}", event);
        try {
            Long bookingId = Long.valueOf(event.get("bookingId").toString());
            String reason = (String) event.getOrDefault("status", "Unknown Error");

            bookingService.handlePaymentFailure(bookingId, reason);

        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }
}