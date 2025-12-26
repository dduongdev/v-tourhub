package com.v_tourhub.review_service.repository;

import com.v_tourhub.review_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Tìm reviews theo destination
    Page<Review> findByDestinationIdAndStatus(Long destinationId, Review.ReviewStatus status, Pageable pageable);
    
    // Tìm reviews theo service
    Page<Review> findByServiceIdAndStatus(Long serviceId, Review.ReviewStatus status, Pageable pageable);
    
    // Tìm reviews của user
    Page<Review> findByUserId(Long userId, Pageable pageable);
    
    // Tính average rating và total reviews cho destination
    @Query("SELECT AVG(r.rating) as avgRating, COUNT(r) as totalReviews " +
           "FROM Review r WHERE r.destinationId = :destinationId AND r.status = 'ACTIVE'")
    Object[] getDestinationRatingStats(@Param("destinationId") Long destinationId);
    
    // Tính average rating và total reviews cho service
    @Query("SELECT AVG(r.rating) as avgRating, COUNT(r) as totalReviews " +
           "FROM Review r WHERE r.serviceId = :serviceId AND r.status = 'ACTIVE'")
    Object[] getServiceRatingStats(@Param("serviceId") Long serviceId);
    
    // Kiểm tra user đã review chưa
    boolean existsByUserIdAndDestinationId(Long userId, Long destinationId);
    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);
    
    // Lấy review của user cho destination/service
    Optional<Review> findByUserIdAndDestinationId(Long userId, Long destinationId);
    Optional<Review> findByUserIdAndServiceId(Long userId, Long serviceId);
}

