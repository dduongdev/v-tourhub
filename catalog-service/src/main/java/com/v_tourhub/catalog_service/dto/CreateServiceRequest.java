package com.v_tourhub.catalog_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

import com.v_tourhub.catalog_service.entity.TourismService;

@Data
public class CreateServiceRequest {
    @NotNull
    private String name;
    
    private String description;
    
    @NotNull
    private BigDecimal price;
    
    private Boolean availability = true;
    
    @NotNull
    private TourismService.ServiceType type; 

    private Map<String, String> attributes;
}