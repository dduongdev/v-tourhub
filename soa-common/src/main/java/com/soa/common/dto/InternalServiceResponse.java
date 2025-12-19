package com.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean availability;
    private String type;
    private String destinationName; 
    private String imageUrl;
}