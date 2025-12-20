package com.soa.review.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long id;
    private Long tourId;
    private Long userId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}
