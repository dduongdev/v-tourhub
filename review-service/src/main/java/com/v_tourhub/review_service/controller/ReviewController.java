package com.v_tourhub.review_service.controller;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.review_service.entity.Review;
import com.v_tourhub.review_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/destinations/{destinationId}")
    public ApiResponse<Page<Review>> getReviewsByDestination(
            @PathVariable Long destinationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Page<Review> reviews = reviewService.getReviewsByDestination(
                destinationId, PageRequest.of(page, size, sort));
        return ApiResponse.success(reviews);
    }

    @GetMapping("/services/{serviceId}")
    public ApiResponse<Page<Review>> getReviewsByService(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Page<Review> reviews = reviewService.getReviewsByService(
                serviceId, PageRequest.of(page, size, sort));
        return ApiResponse.success(reviews);
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<Page<Review>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<Review> reviews = reviewService.getReviewsByUser(
                userId, PageRequest.of(page, size));
        return ApiResponse.success(reviews);
    }

    @GetMapping("/{id}")
    public ApiResponse<Review> getReview(@PathVariable Long id) {
        return ApiResponse.success(reviewService.getReviewById(id));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ApiResponse<Review> createReview(@RequestBody Review review) {
        return ApiResponse.success(reviewService.createReview(review));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<Review> updateReview(
            @PathVariable Long id, 
            @RequestBody Review review) {
        return ApiResponse.success(reviewService.updateReview(id, review));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ApiResponse.success(null, "Review deleted successfully");
    }

    @GetMapping("/destinations/{destinationId}/stats")
    public ApiResponse<Object[]> getDestinationStats(@PathVariable Long destinationId) {
        return ApiResponse.success(reviewService.getDestinationRatingStats(destinationId));
    }

    @GetMapping("/services/{serviceId}/stats")
    public ApiResponse<Object[]> getServiceStats(@PathVariable Long serviceId) {
        return ApiResponse.success(reviewService.getServiceRatingStats(serviceId));
    }
}

