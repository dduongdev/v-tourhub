package com.v_tourhub.catalog_service.scheduler;

import com.soa.common.event.InventoryLockFailedEvent;
import com.v_tourhub.catalog_service.config.RabbitMQConfig;
import com.v_tourhub.catalog_service.entity.OutboxEvent;
import com.v_tourhub.catalog_service.repository.OutboxEventRepository;

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

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepo.findByIsPublishedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        log.info("[Catalog] Found {} events in outbox to publish.", events.size());

        for (OutboxEvent event : events) {
            try {
                // Lấy tên class đầy đủ
                String typeId = getTypeIdForEventType(event.getEventType());

                // Tạo MessageProperties
                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
                messageProperties.setHeader("__TypeId__", typeId);

                // Tạo Message
                Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), messageProperties);

                // Gửi
                rabbitTemplate.send(RabbitMQConfig.EXCHANGE, event.getEventType(), message);
                
                event.setPublished(true);

            } catch (Exception e) {
                log.error("[Catalog] Failed to publish outbox event ID {}:", event.getId(), e);
            }
        }
    }

    private String getTypeIdForEventType(String eventType) {
        if (RabbitMQConfig.ROUTING_KEY_INVENTORY_LOCK_FAILED.equals(eventType)) {
            return InventoryLockFailedEvent.class.getName();
        }
        if (RabbitMQConfig.ROUTING_KEY_INVENTORY_LOCK_SUCCESSFUL.equals(eventType)) {
            return com.soa.common.event.InventoryLockSuccessfulEvent.class.getName();
        };
        throw new IllegalArgumentException("Unknown event type for Catalog Service: " + eventType);
    }
}