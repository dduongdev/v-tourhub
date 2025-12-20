package com.v_tourhub.booking_service.listener;

import com.soa.common.event.InventoryLockFailedEvent;
import com.soa.common.event.InventoryLockSuccessfulEvent;
import com.v_tourhub.booking_service.config.RabbitMQConfig;
import com.v_tourhub.booking_service.service.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventListener {

    private final BookingService bookingService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_LOCK_FAILED)
    public void handleInventoryLockFailed(InventoryLockFailedEvent event) {
        log.info("Received InventoryLockFailedEvent: {}", event);
        try {
            bookingService.handleInventoryLockFailure(event);
        } catch (Exception e) {
            log.error("Error processing inventory lock failed event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INVENTORY_LOCK_SUCCESSFUL)
    public void handleInventoryLockSuccessful(InventoryLockSuccessfulEvent event) {
        log.info("Received InventoryLockSuccessfulEvent: {}", event);
        try {
            bookingService.moveToPendingPayment(event.getBookingId());
        } catch (Exception e) {
            log.error("Error processing inventory lock successful event", e);
        }
    }
}