package com.soa.payment_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.soa.payment_service.config.RabbitMQConfig;
import com.soa.payment_service.service.PaymentService;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_PROCESS)
    public void handleBookingCreated(Map<String, Object> event) {
        log.info("Received booking.created event: {}", event);
        try {
            Long bookingId = Long.valueOf(event.get("bookingId").toString());
            String userId = null;
            if (event.get("userId") != null) {
                 try { userId = String.valueOf(event.get("userId").toString()); } catch (Exception e) {}
            }
            
            BigDecimal amount = new BigDecimal(event.get("amount").toString());

            paymentService.initPayment(bookingId, userId, amount);

        } catch (Exception e) {
            log.error("Error processing booking event", e);
        }
    }
}