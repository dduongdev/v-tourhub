package com.soa.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {
    @NotNull
    private Long tourId;

    @NotNull
    private Long userId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}
