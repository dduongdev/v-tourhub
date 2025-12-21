package com.soa.analytics.controller;

import com.soa.analytics.dto.AnalyticsSummaryDto;
import com.soa.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/tour/{tourId}/summary")
    public ResponseEntity<AnalyticsSummaryDto> summary(@PathVariable Long tourId) {
        return ResponseEntity.ok(service.summary(tourId));
    }

    @GetMapping("/tour/{tourId}/distribution")
    public ResponseEntity<Map<Integer, Long>> distribution(@PathVariable Long tourId) {
        return ResponseEntity.ok(service.ratingDistribution(tourId));
    }
}
