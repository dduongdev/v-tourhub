package com.soa.analytics.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDto {
    private Long tourId;
    private Long totalReviews;
    private Double averageRating;
}
