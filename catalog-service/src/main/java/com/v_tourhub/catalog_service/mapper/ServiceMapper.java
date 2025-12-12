package com.v_tourhub.catalog_service.mapper;


import org.springframework.stereotype.Component;

import com.v_tourhub.catalog_service.dto.CreateServiceRequest;
import com.v_tourhub.catalog_service.dto.TourismServiceResponse;
import com.v_tourhub.catalog_service.entity.TourismService;

@Component
public class ServiceMapper {
    public TourismServiceResponse toResponse(TourismService entity) {
        if (entity == null) return null;
        return TourismServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .availability(entity.getAvailability())
                .providerId(entity.getProviderId())
                .type(entity.getType().name())
                .destinationName(entity.getDestination() != null ? entity.getDestination().getName() : null)
                .build();
    }

    public TourismService toEntity(CreateServiceRequest request) {
        if (request == null) return null;
        return TourismService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .availability(request.getAvailability())
                .providerId(request.getProviderId())
                .type(request.getType())
                .build();
    }
}