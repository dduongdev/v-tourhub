package com.v_tourhub.booking_service.client;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.booking_service.dto.CatalogServiceDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/catalog/destinations/{id}")
    ApiResponse<Object> getDestination(@PathVariable("id") Long id);

    @GetMapping("/api/catalog/services/detail/{id}") 
    ApiResponse<CatalogServiceDto> getServiceDetail(@PathVariable("id") Long id);
}