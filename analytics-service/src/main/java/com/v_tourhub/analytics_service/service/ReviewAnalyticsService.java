package com.v_tourhub.analytics_service.service;

import com.v_tourhub.analytics_service.entity.ReviewAnalytics;
import com.v_tourhub.analytics_service.repository.ReviewAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAnalyticsService {

    private final ReviewAnalyticsRepository analyticsRepository;

    @Transactional
    public void handleReviewCreated(Long destinationId, Long serviceId, 
                                    ReviewAnalytics.ReviewType reviewType, Integer rating) {
        ReviewAnalytics analytics = getOrCreateAnalytics(destinationId, serviceId, reviewType);
        
        // Update statistics
        analytics.setTotalReviews(analytics.getTotalReviews() + 1);
        analytics.incrementRatingCount(rating);
        
        // Recalculate average rating
        recalculateAverageRating(analytics);
        
        analyticsRepository.save(analytics);
        log.info("Review analytics updated for {}: avgRating={}, totalReviews={}", 
                reviewType, analytics.getAverageRating(), analytics.getTotalReviews());
    }

    @Transactional
    public void handleReviewUpdated(Long destinationId, Long serviceId,
                                   ReviewAnalytics.ReviewType reviewType, 
                                   Integer oldRating, Integer newRating) {
        ReviewAnalytics analytics = getOrCreateAnalytics(destinationId, serviceId, reviewType);
        
        // Update rating distribution
        if (oldRating != null && !oldRating.equals(newRating)) {
            analytics.decrementRatingCount(oldRating);
            analytics.incrementRatingCount(newRating);
        }
        
        // Recalculate average rating
        recalculateAverageRating(analytics);
        
        analyticsRepository.save(analytics);
        log.info("Review analytics updated after review update for {}: avgRating={}", 
                reviewType, analytics.getAverageRating());
    }

    @Transactional
    public void handleReviewDeleted(Long destinationId, Long serviceId,
                                   ReviewAnalytics.ReviewType reviewType, Integer rating) {
        Optional<ReviewAnalytics> optional = findAnalytics(destinationId, serviceId, reviewType);
        
        if (optional.isEmpty()) {
            log.warn("Analytics not found for deleted review: {} - {}", reviewType, 
                    destinationId != null ? destinationId : serviceId);
            return;
        }
        
        ReviewAnalytics analytics = optional.get();
        
        // Update statistics
        analytics.setTotalReviews(Math.max(0, analytics.getTotalReviews() - 1));
        analytics.decrementRatingCount(rating);
        
        // Recalculate average rating
        recalculateAverageRating(analytics);
        
        analyticsRepository.save(analytics);
        log.info("Review analytics updated after review deletion for {}: avgRating={}, totalReviews={}", 
                reviewType, analytics.getAverageRating(), analytics.getTotalReviews());
    }

    public ReviewAnalytics getAnalyticsByDestination(Long destinationId) {
        return analyticsRepository
                .findByDestinationIdAndReviewType(destinationId, ReviewAnalytics.ReviewType.DESTINATION)
                .orElse(createEmptyAnalytics(destinationId, null, ReviewAnalytics.ReviewType.DESTINATION));
    }

    public ReviewAnalytics getAnalyticsByService(Long serviceId) {
        return analyticsRepository
                .findByServiceIdAndReviewType(serviceId, ReviewAnalytics.ReviewType.SERVICE)
                .orElse(createEmptyAnalytics(null, serviceId, ReviewAnalytics.ReviewType.SERVICE));
    }

    private ReviewAnalytics getOrCreateAnalytics(Long destinationId, Long serviceId,
                                                ReviewAnalytics.ReviewType reviewType) {
        Optional<ReviewAnalytics> existing = findAnalytics(destinationId, serviceId, reviewType);
        return existing.orElseGet(() -> createEmptyAnalytics(destinationId, serviceId, reviewType));
    }

    private Optional<ReviewAnalytics> findAnalytics(Long destinationId, Long serviceId,
                                                    ReviewAnalytics.ReviewType reviewType) {
        if (reviewType == ReviewAnalytics.ReviewType.DESTINATION && destinationId != null) {
            return analyticsRepository.findByDestinationIdAndReviewType(destinationId, reviewType);
        } else if (reviewType == ReviewAnalytics.ReviewType.SERVICE && serviceId != null) {
            return analyticsRepository.findByServiceIdAndReviewType(serviceId, reviewType);
        }
        return Optional.empty();
    }

    private ReviewAnalytics createEmptyAnalytics(Long destinationId, Long serviceId,
                                                ReviewAnalytics.ReviewType reviewType) {
        return ReviewAnalytics.builder()
                .destinationId(destinationId)
                .serviceId(serviceId)
                .reviewType(reviewType)
                .averageRating(0.0)
                .totalReviews(0)
                .rating5Count(0)
                .rating4Count(0)
                .rating3Count(0)
                .rating2Count(0)
                .rating1Count(0)
                .build();
    }

    private void recalculateAverageRating(ReviewAnalytics analytics) {
        int total = analytics.getTotalReviews();
        if (total == 0) {
            analytics.setAverageRating(0.0);
            return;
        }

        double sum = (analytics.getRating5Count() * 5.0) +
                    (analytics.getRating4Count() * 4.0) +
                    (analytics.getRating3Count() * 3.0) +
                    (analytics.getRating2Count() * 2.0) +
                    (analytics.getRating1Count() * 1.0);

        analytics.setAverageRating(Math.round((sum / total) * 100.0) / 100.0); // Round to 2 decimals
    }
}

