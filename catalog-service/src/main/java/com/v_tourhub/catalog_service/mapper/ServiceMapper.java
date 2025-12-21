package com.v_tourhub.catalog_service.mapper;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.soa.common.dto.InternalServiceResponse;
import com.v_tourhub.catalog_service.dto.CreateServiceRequest;
import com.v_tourhub.catalog_service.dto.PublicTourismServiceDTO;
import com.v_tourhub.catalog_service.dto.PublicTourismServiceDTO.InventoryInfo;
import com.v_tourhub.catalog_service.entity.Attribute;
import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.service.MediaService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceMapper {
    private final MediaService mediaService;

    public InternalServiceResponse toInternalResponse(TourismService entity) {
        if (entity == null) return null;
        return InternalServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .availability(entity.getAvailability())
                .destinationName(entity.getDestination() != null ? entity.getDestination().getName() : null)
                .type(entity.getType() != null ? entity.getType().name() : null)
                .build();
    }

    public TourismService toEntity(CreateServiceRequest request) {
        if (request == null) return null;
        TourismService service = TourismService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .availability(request.getAvailability())
                .type(request.getType())
                .build();
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            List<Attribute> attributeEntities = new ArrayList<>();
            for (Map.Entry<String, String> entry : request.getAttributes().entrySet()) {
                Attribute attr = Attribute.builder()
                        .attributeKey(entry.getKey())
                        .attributeValue(entry.getValue())
                        .tourismService(service) 
                        .build();
                attributeEntities.add(attr);
            }
            service.setAttributes(attributeEntities);
        }

        return service;
    }

    public PublicTourismServiceDTO toPublicDTO(TourismService entity, List<Inventory> inventories) {
        if (entity == null) return null;

        List<InventoryInfo> inventoryInfos = Collections.emptyList();
        if (inventories != null && !inventories.isEmpty()) {
            inventoryInfos = inventories.stream().map(inv -> 
                InventoryInfo.builder()
                    .date(inv.getDate())
                    .availableStock(inv.getAvailableStock())
                    .totalStock(inv.getTotalStock())
                    .isAvailable(inv.getAvailableStock() > 0)
                    .build()
            ).collect(Collectors.toList());
        }

        PublicTourismServiceDTO.DestinationInfo destInfo = null;
        if (entity.getDestination() != null) {
            destInfo = PublicTourismServiceDTO.DestinationInfo.builder()
                    .id(entity.getDestination().getId())
                    .name(entity.getDestination().getName())
                    .city(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getCity() : null)
                    .address(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getAddress() : null)
                    .province(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getProvince() : null)
                    .latitude(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getLatitude() : null)
                    .longitude(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getLongitude() : null)
                    .build();
        }

        return PublicTourismServiceDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .serviceType(entity.getType() != null ? entity.getType().name() : null)
                .availability(entity.getAvailability())
                .destination(destInfo)
                .inventoryCalendar(inventoryInfos)
                .mediaUrls(entity.getMediaList() != null ?
                        entity.getMediaList().stream()
                                .map(media -> mediaService.getFileUrl(media.getUrl()))
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .attributes(entity.getAttributes() != null ?
                        entity.getAttributes().stream()
                                .collect(Collectors.toMap(Attribute::getAttributeKey, Attribute::getAttributeValue))
                        : Collections.emptyMap())
                .build();
    }

    public PublicTourismServiceDTO toPublicDTO(TourismService entity) {
        if (entity == null) return null;

        PublicTourismServiceDTO.DestinationInfo destInfo = null;
        if (entity.getDestination() != null) {
            destInfo = PublicTourismServiceDTO.DestinationInfo.builder()
                    .id(entity.getDestination().getId())
                    .name(entity.getDestination().getName())
                    .city(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getCity() : null)
                    .address(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getAddress() : null)
                    .province(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getProvince() : null)
                    .latitude(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getLatitude() : null)
                    .longitude(entity.getDestination().getLocation() != null ? entity.getDestination().getLocation().getLongitude() : null)
                    .build();
        }

        return PublicTourismServiceDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .serviceType(entity.getType() != null ? entity.getType().name() : null)
                .availability(entity.getAvailability())
                .destination(destInfo)
                .mediaUrls(entity.getMediaList() != null ?
                        entity.getMediaList().stream()
                                .map(media -> mediaService.getFileUrl(media.getUrl()))
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .attributes(entity.getAttributes() != null ?
                        entity.getAttributes().stream()
                                .collect(Collectors.toMap(Attribute::getAttributeKey, Attribute::getAttributeValue))
                        : Collections.emptyMap())
                .build();
    }
}