package com.v_tourhub.catalog_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TourismServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean availability;
    private Long providerId;
    private String type;
    private String destinationName; 
    private String imageUrl;
}