package com.v_tourhub.catalog_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Valid 
    private InventoryConfig inventory;

    @Data
    public static class InventoryConfig {
        @NotNull
        @Min(value = 0, message = "Total stock must be non-negative")
        private Integer totalStock;

        @NotNull
        @FutureOrPresent
        private LocalDate startDate;
        
        @NotNull
        private LocalDate endDate;
    }
}