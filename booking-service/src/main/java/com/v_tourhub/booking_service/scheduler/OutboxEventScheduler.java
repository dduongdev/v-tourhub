package com.v_tourhub.booking_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soa.common.event.*;
import com.v_tourhub.booking_service.config.RabbitMQConfig;
import com.v_tourhub.booking_service.entity.OutboxEvent;
import com.v_tourhub.booking_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxRepo;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepo.findByIsPublishedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) return;

        log.info("Found {} events in outbox to publish.", events.size());

        for (OutboxEvent event : events) {
            try {
                // --- THAY ĐỔI CỐT LÕI NẰM Ở ĐÂY ---

                // 1. Lấy tên class đầy đủ (ví dụ: "com.soa.common.event.BookingCreatedEvent")
                String typeId = getTypeIdForEventType(event.getEventType());

                // 2. Tạo MessageProperties
                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
                // Set header __TypeId__ mà Jackson2JsonMessageConverter cần
                messageProperties.setHeader("__TypeId__", typeId);

                // 3. Tạo Message
                Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), messageProperties);

                // 4. Gửi Message (không convert nữa)
                rabbitTemplate.send(RabbitMQConfig.EXCHANGE, event.getEventType(), message);
                
                // 5. Đánh dấu đã gửi
                event.setPublished(true);
            } catch (Exception e) {
                log.error("Failed to publish outbox event ID {}:", event.getId(), e);
            }
        }
    }

    /**
     * Map từ eventType (routing_key) sang tên Class đầy đủ
     */
    private String getTypeIdForEventType(String eventType) {
        return switch (eventType) {
            case "booking.created" -> BookingCreatedEvent.class.getName();
            case "booking.confirmed" -> BookingConfirmedEvent.class.getName();
            case "booking.cancelled" -> BookingCancelledEvent.class.getName();
            case "booking.failed" -> BookingFailedEvent.class.getName();
            case "payment.completed" -> PaymentCompletedEvent.class.getName();
            case "payment.failed" -> PaymentFailedEvent.class.getName();
            case "inventory.lock.failed" -> InventoryLockFailedEvent.class.getName();
            case "booking.ready_for_payment" -> BookingReadyForPaymentEvent.class.getName();
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}