package com.soa.review.repository;

import com.soa.review.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTourId(Long tourId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tourId = :tourId")
    Double findAverageRatingByTourId(@Param("tourId") Long tourId);
}
