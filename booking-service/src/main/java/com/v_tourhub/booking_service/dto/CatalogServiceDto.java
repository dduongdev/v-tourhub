package com.v_tourhub.booking_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CatalogServiceDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Boolean availability;
    private Long providerId;
}