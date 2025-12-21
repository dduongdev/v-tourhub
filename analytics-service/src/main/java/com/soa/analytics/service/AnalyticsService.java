package com.soa.analytics.service;

import com.soa.analytics.client.ReviewClient;
import com.soa.analytics.client.ReviewClientDto;
import com.soa.analytics.dto.AnalyticsSummaryDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final ReviewClient reviewClient;

    public AnalyticsService(ReviewClient reviewClient) {
        this.reviewClient = reviewClient;
    }

    public AnalyticsSummaryDto summary(Long tourId) {
        List<ReviewClientDto> reviews = reviewClient.findByTourId(tourId);
        long total = reviews.size();
        double avg = reviews.stream().mapToInt(r -> r.getRating() == null ? 0 : r.getRating()).average().orElse(0.0);
        return AnalyticsSummaryDto.builder().tourId(tourId).totalReviews(total).averageRating(Math.round(avg * 100.0)/100.0).build();
    }

    public Map<Integer, Long> ratingDistribution(Long tourId) {
        List<ReviewClientDto> reviews = reviewClient.findByTourId(tourId);
        return reviews.stream().collect(Collectors.groupingBy(r -> r.getRating() == null ? 0 : r.getRating(), Collectors.counting()));
    }
}
