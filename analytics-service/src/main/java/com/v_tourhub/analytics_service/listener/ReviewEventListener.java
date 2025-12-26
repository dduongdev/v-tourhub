package com.v_tourhub.analytics_service.listener;

import com.soa.common.util.AppConstants;
import com.v_tourhub.analytics_service.entity.ReviewAnalytics;
import com.v_tourhub.analytics_service.service.ReviewAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final ReviewAnalyticsService analyticsService;

    @RabbitListener(queues = AppConstants.QUEUE_REVIEW_CREATED)
    public void handleReviewCreated(Map<String, Object> reviewEvent) {
        try {
            log.info("Received review created event: {}", reviewEvent);
            
            Long destinationId = getLongValue(reviewEvent, "destinationId");
            Long serviceId = getLongValue(reviewEvent, "serviceId");
            String reviewTypeStr = (String) reviewEvent.get("reviewType");
            Integer rating = getIntegerValue(reviewEvent, "rating");
            
            ReviewAnalytics.ReviewType reviewType = 
                    "DESTINATION".equals(reviewTypeStr) 
                    ? ReviewAnalytics.ReviewType.DESTINATION 
                    : ReviewAnalytics.ReviewType.SERVICE;
            
            analyticsService.handleReviewCreated(destinationId, serviceId, reviewType, rating);
        } catch (Exception e) {
            log.error("Error processing review created event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = AppConstants.QUEUE_REVIEW_UPDATED)
    public void handleReviewUpdated(Map<String, Object> reviewEvent) {
        try {
            log.info("Received review updated event: {}", reviewEvent);
            
            Long destinationId = getLongValue(reviewEvent, "destinationId");
            Long serviceId = getLongValue(reviewEvent, "serviceId");
            String reviewTypeStr = (String) reviewEvent.get("reviewType");
            Integer rating = getIntegerValue(reviewEvent, "rating");
            Integer oldRating = getIntegerValue(reviewEvent, "oldRating");
            
            ReviewAnalytics.ReviewType reviewType = 
                    "DESTINATION".equals(reviewTypeStr) 
                    ? ReviewAnalytics.ReviewType.DESTINATION 
                    : ReviewAnalytics.ReviewType.SERVICE;
            
            analyticsService.handleReviewUpdated(destinationId, serviceId, reviewType, oldRating, rating);
        } catch (Exception e) {
            log.error("Error processing review updated event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = AppConstants.QUEUE_REVIEW_DELETED)
    public void handleReviewDeleted(Map<String, Object> reviewEvent) {
        try {
            log.info("Received review deleted event: {}", reviewEvent);
            
            Long destinationId = getLongValue(reviewEvent, "destinationId");
            Long serviceId = getLongValue(reviewEvent, "serviceId");
            String reviewTypeStr = (String) reviewEvent.get("reviewType");
            Integer rating = getIntegerValue(reviewEvent, "rating");
            
            ReviewAnalytics.ReviewType reviewType = 
                    "DESTINATION".equals(reviewTypeStr) 
                    ? ReviewAnalytics.ReviewType.DESTINATION 
                    : ReviewAnalytics.ReviewType.SERVICE;
            
            analyticsService.handleReviewDeleted(destinationId, serviceId, reviewType, rating);
        } catch (Exception e) {
            log.error("Error processing review deleted event: {}", e.getMessage(), e);
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }
}

