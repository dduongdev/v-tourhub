package com.v_tourhub.catalog_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.v_tourhub.catalog_service.entity.Attribute;
import com.v_tourhub.catalog_service.entity.TourismService;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class UpdateServiceRequest {
    private String name;

    private String description;

    private BigDecimal price;
    
    private Boolean availability;

    private TourismService.ServiceType type;
    private Set<Attribute> attributes;
}
