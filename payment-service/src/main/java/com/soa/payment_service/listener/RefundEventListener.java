package com.soa.payment_service.listener;

import com.soa.common.event.RefundRequestedEvent;
import com.soa.payment_service.config.RabbitMQConfig;
import com.soa.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefundEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_REFUND_REQUESTED)
    public void handleRefundRequested(RefundRequestedEvent event) {
        log.info("Received RefundRequestedEvent: {}", event);
        try {
            paymentService.initiateRefund(
                    event.getBookingId(),
                    event.getReason(),
                    event.getRefundAmount());
        } catch (Exception e) {
            log.error("Error processing refund requested event for booking {}",
                    event.getBookingId(), e);
            // In production, you might want to send to DLQ or retry
        }
    }
}
