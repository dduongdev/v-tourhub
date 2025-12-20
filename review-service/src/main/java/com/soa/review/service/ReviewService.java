package com.soa.review.service;

import com.soa.review.dto.CreateReviewRequest;
import com.soa.review.model.Review;
import com.soa.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository repository;

    public ReviewService(ReviewRepository repository) {
        this.repository = repository;
    }

    public Review create(CreateReviewRequest req) {
        Review r = Review.builder()
                .tourId(req.getTourId())
                .userId(req.getUserId())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        return repository.save(r);
    }

    public Review get(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Review not found"));
    }

    public List<Review> listByTour(Long tourId) {
        return repository.findByTourId(tourId);
    }

    public Double avgRating(Long tourId) {
        Double v = repository.findAverageRatingByTourId(tourId);
        return v == null ? 0.0 : v;
    }

    public Review update(Long id, CreateReviewRequest req) {
        Review r = get(id);
        r.setRating(req.getRating());
        r.setComment(req.getComment());
        // don't change tourId or userId to keep ownership simple
        return repository.save(r);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
