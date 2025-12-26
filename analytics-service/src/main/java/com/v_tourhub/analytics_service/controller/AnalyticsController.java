package com.v_tourhub.analytics_service.controller;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.analytics_service.entity.ReviewAnalytics;
import com.v_tourhub.analytics_service.service.ReviewAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ReviewAnalyticsService analyticsService;

    @GetMapping("/reviews/destinations/{destinationId}")
    public ApiResponse<ReviewAnalytics> getDestinationAnalytics(@PathVariable Long destinationId) {
        return ApiResponse.success(analyticsService.getAnalyticsByDestination(destinationId));
    }

    @GetMapping("/reviews/services/{serviceId}")
    public ApiResponse<ReviewAnalytics> getServiceAnalytics(@PathVariable Long serviceId) {
        return ApiResponse.success(analyticsService.getAnalyticsByService(serviceId));
    }
}

