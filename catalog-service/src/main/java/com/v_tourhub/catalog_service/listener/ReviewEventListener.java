package com.v_tourhub.catalog_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.v_tourhub.catalog_service.service.CatalogService;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewEventListener {

    private final CatalogService catalogService;

    @RabbitListener(queues = "${spring.rabbitmq.template.default-receive-queue:review.created.queue}")
    public void handleReviewCreated(Map<String, Object> event) {
        log.info("Received review event: {}", event);
        
        try {
            Long entityId = Long.valueOf(event.get("entityId").toString());
            String entityType = event.get("entityType").toString();
            Double newAverage = Double.valueOf(event.get("newAverage").toString());
            Integer totalCount = Integer.valueOf(event.get("totalCount").toString());

            if ("DESTINATION".equalsIgnoreCase(entityType)) {
                catalogService.updateRating(entityId, newAverage, totalCount);
            }
        } catch (Exception e) {
            log.error("Error processing review event", e);
        }
    }
}