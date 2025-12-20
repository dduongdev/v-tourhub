package com.soa.payment_service.scheduler;

import com.soa.common.event.PaymentCompletedEvent;
import com.soa.common.event.PaymentFailedEvent;
import com.soa.payment_service.entity.OutboxEvent;
import com.soa.payment_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
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

    private static final String BOOKING_EXCHANGE = "booking.exchange";

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepo.findByIsPublishedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        log.info("[Payment] Found {} events in outbox to publish.", events.size());

        for (OutboxEvent event : events) {
            try {
                // Lấy tên class đầy đủ từ eventType (routing_key)
                String typeId = getTypeIdForEventType(event.getEventType());

                // Tạo MessageProperties và set header __TypeId__
                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
                messageProperties.setHeader("__TypeId__", typeId);

                // Tạo Message với payload là JSON String
                Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), messageProperties);

                // Gửi message (không convert)
                rabbitTemplate.send(BOOKING_EXCHANGE, event.getEventType(), message);
                
                // Đánh dấu đã gửi
                event.setPublished(true);

            } catch (Exception e) {
                log.error("[Payment] Failed to publish outbox event ID {}:", event.getId(), e);
            }
        }
    }

    private String getTypeIdForEventType(String eventType) {
        return switch (eventType) {
            case "payment.completed" -> PaymentCompletedEvent.class.getName();
            case "payment.failed" -> PaymentFailedEvent.class.getName();
            default -> throw new IllegalArgumentException("Unknown event type for Payment Service: " + eventType);
        };
    }
}