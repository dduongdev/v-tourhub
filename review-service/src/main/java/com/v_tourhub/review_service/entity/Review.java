package com.v_tourhub.review_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_destination_id", columnList = "destination_id"),
    @Index(name = "idx_service_id", columnList = "service_id"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE reviews SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Review extends BaseEntity {

    @Column(nullable = false)
    private Long userId; // ID của user từ userprofile-service

    @Column(name = "destination_id")
    private Long destinationId; // ID của destination từ catalog-service

    @Column(name = "service_id")
    private Long serviceId; // ID của service từ catalog-service

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewType reviewType; // DESTINATION hoặc SERVICE

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false; // Review đã được verify chưa

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.ACTIVE;

    public enum ReviewType {
        DESTINATION, SERVICE
    }

    public enum ReviewStatus {
        ACTIVE, HIDDEN, REPORTED
    }
}

