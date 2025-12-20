package com.soa.review.controller;

import com.soa.review.dto.CreateReviewRequest;
import com.soa.review.dto.ReviewDto;
import com.soa.review.model.Review;
import com.soa.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody CreateReviewRequest req) {
        Review r = service.create(req);
        return ResponseEntity.ok(toDto(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDto> update(@PathVariable Long id, @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.ok(toDto(service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tour/{tourId}")
    public ResponseEntity<List<ReviewDto>> listByTour(@PathVariable Long tourId) {
        return ResponseEntity.ok(service.listByTour(tourId).stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/tour/{tourId}/average")
    public ResponseEntity<Double> avg(@PathVariable Long tourId) {
        return ResponseEntity.ok(service.avgRating(tourId));
    }

    private ReviewDto toDto(Review r) {
        return ReviewDto.builder()
                .id(r.getId())
                .tourId(r.getTourId())
                .userId(r.getUserId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
