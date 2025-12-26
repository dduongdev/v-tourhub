package com.v_tourhub.analytics_service.repository;

import com.v_tourhub.analytics_service.entity.ReviewAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewAnalyticsRepository extends JpaRepository<ReviewAnalytics, Long> {
    
    Optional<ReviewAnalytics> findByDestinationIdAndReviewType(
            Long destinationId, ReviewAnalytics.ReviewType reviewType);
    
    Optional<ReviewAnalytics> findByServiceIdAndReviewType(
            Long serviceId, ReviewAnalytics.ReviewType reviewType);
}

