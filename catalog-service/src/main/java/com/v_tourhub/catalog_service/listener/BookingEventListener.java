package com.v_tourhub.catalog_service.listener;

import com.soa.common.event.*;
import com.v_tourhub.catalog_service.config.RabbitMQConfig;
import com.v_tourhub.catalog_service.service.InventoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CREATED)
    public void handleBookingCreated(BookingCreatedEvent event) {
        inventoryService.lockInventory(event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CONFIRMED)
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        inventoryService.commitInventory(event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BOOKING_CANCELLED)
    public void handleBookingCancelled(BookingCancelledEvent event) {
        inventoryService.releaseInventory(event);
    }
}