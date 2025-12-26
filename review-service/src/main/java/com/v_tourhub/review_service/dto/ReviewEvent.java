package com.v_tourhub.review_service.dto;

import com.v_tourhub.review_service.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEvent {
    private Long reviewId;
    private Long userId;
    private Long destinationId;
    private Long serviceId;
    private Review.ReviewType reviewType;
    private Integer rating;
    private Integer oldRating; // Cho UPDATE event
    private String eventType; // CREATED, UPDATED, DELETED
    private LocalDateTime timestamp;
}

