package com.v_tourhub.review_service.service;

import com.soa.common.util.AppConstants;
import com.v_tourhub.review_service.dto.ReviewEvent;
import com.v_tourhub.review_service.entity.Review;
import com.v_tourhub.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RabbitTemplate rabbitTemplate;

    public Page<Review> getReviewsByDestination(Long destinationId, Pageable pageable) {
        return reviewRepository.findByDestinationIdAndStatus(
                destinationId, Review.ReviewStatus.ACTIVE, pageable);
    }

    public Page<Review> getReviewsByService(Long serviceId, Pageable pageable) {
        return reviewRepository.findByServiceIdAndStatus(
                serviceId, Review.ReviewStatus.ACTIVE, pageable);
    }

    public Page<Review> getReviewsByUser(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable);
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
    }

    @Transactional
    public Review createReview(Review review) {
        // Validate: User chỉ được review 1 lần cho mỗi destination/service
        if (review.getReviewType() == Review.ReviewType.DESTINATION) {
            if (reviewRepository.existsByUserIdAndDestinationId(
                    review.getUserId(), review.getDestinationId())) {
                throw new RuntimeException("User already reviewed this destination");
            }
        } else {
            if (reviewRepository.existsByUserIdAndServiceId(
                    review.getUserId(), review.getServiceId())) {
                throw new RuntimeException("User already reviewed this service");
            }
        }

        Review savedReview = reviewRepository.save(review);
        
        // Publish event
        publishReviewEvent(savedReview, "CREATED");
        
        return savedReview;
    }

    @Transactional
    public Review updateReview(Long id, Review reviewDetails) {
        Review existing = getReviewById(id);
        
        Integer oldRating = existing.getRating(); // Lưu old rating
        
        existing.setRating(reviewDetails.getRating());
        existing.setComment(reviewDetails.getComment());
        existing.setStatus(reviewDetails.getStatus());
        
        Review updatedReview = reviewRepository.save(existing);
        
        // Publish event với oldRating
        publishReviewEvent(updatedReview, "UPDATED", oldRating);
        
        return updatedReview;
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = getReviewById(id);
        reviewRepository.deleteById(id);
        
        // Publish event
        publishReviewEvent(review, "DELETED");
    }

    public Object[] getDestinationRatingStats(Long destinationId) {
        return reviewRepository.getDestinationRatingStats(destinationId);
    }

    public Object[] getServiceRatingStats(Long serviceId) {
        return reviewRepository.getServiceRatingStats(serviceId);
    }

    private void publishReviewEvent(Review review, String eventType) {
        publishReviewEvent(review, eventType, null);
    }

    private void publishReviewEvent(Review review, String eventType, Integer oldRating) {
        ReviewEvent.ReviewEventBuilder builder = ReviewEvent.builder()
                .reviewId(review.getId())
                .userId(review.getUserId())
                .destinationId(review.getDestinationId())
                .serviceId(review.getServiceId())
                .reviewType(review.getReviewType())
                .rating(review.getRating())
                .eventType(eventType)
                .timestamp(LocalDateTime.now());
        
        if (oldRating != null) {
            builder.oldRating(oldRating);
        }
        
        ReviewEvent event = builder.build();

        String routingKey = switch (eventType) {
            case "CREATED" -> AppConstants.ROUTING_KEY_REVIEW_CREATED;
            case "UPDATED" -> AppConstants.ROUTING_KEY_REVIEW_UPDATED;
            case "DELETED" -> AppConstants.ROUTING_KEY_REVIEW_DELETED;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };

        rabbitTemplate.convertAndSend(AppConstants.EXCHANGE_REVIEW, routingKey, event);
    }
}

