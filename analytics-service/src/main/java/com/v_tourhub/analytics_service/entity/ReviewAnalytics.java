package com.v_tourhub.analytics_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "review_analytics", indexes = {
    @Index(name = "idx_destination_id", columnList = "destination_id"),
    @Index(name = "idx_service_id", columnList = "service_id"),
    @Index(name = "idx_review_type", columnList = "review_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE review_analytics SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class ReviewAnalytics extends BaseEntity {

    @Column(name = "destination_id")
    private Long destinationId;

    @Column(name = "service_id")
    private Long serviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType;

    @Column(nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer rating5Count = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer rating4Count = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer rating3Count = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer rating2Count = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer rating1Count = 0;

    @Column(name = "last_updated_review_id")
    private Long lastUpdatedReviewId;

    public enum ReviewType {
        DESTINATION, SERVICE
    }

    // Helper method để update rating distribution
    public void incrementRatingCount(Integer rating) {
        switch (rating) {
            case 5 -> this.rating5Count++;
            case 4 -> this.rating4Count++;
            case 3 -> this.rating3Count++;
            case 2 -> this.rating2Count++;
            case 1 -> this.rating1Count++;
        }
    }

    public void decrementRatingCount(Integer rating) {
        switch (rating) {
            case 5 -> this.rating5Count = Math.max(0, this.rating5Count - 1);
            case 4 -> this.rating4Count = Math.max(0, this.rating4Count - 1);
            case 3 -> this.rating3Count = Math.max(0, this.rating3Count - 1);
            case 2 -> this.rating2Count = Math.max(0, this.rating2Count - 1);
            case 1 -> this.rating1Count = Math.max(0, this.rating1Count - 1);
        }
    }
}

