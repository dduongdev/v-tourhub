package com.v_tourhub.catalog_service.controller;

import com.soa.common.dto.ApiResponse;
import com.soa.common.dto.InternalServiceResponse;
import com.v_tourhub.catalog_service.dto.CreateServiceRequest;
import com.v_tourhub.catalog_service.dto.PublicTourismServiceDTO;
import com.v_tourhub.catalog_service.entity.Category;
import com.v_tourhub.catalog_service.entity.Destination;
import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.Media;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.mapper.ServiceMapper;
import com.v_tourhub.catalog_service.service.CatalogService;
import com.v_tourhub.catalog_service.service.InventoryService;
import com.v_tourhub.catalog_service.service.MediaService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService service;
    private final ServiceMapper serviceMapper;
    private final MediaService mediaService;
    private final InventoryService inventoryService;

    @GetMapping("/destinations")
    public ApiResponse<Page<Destination>> getDestinations(
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        filters.remove("page");
        filters.remove("size");
        
        Page<Destination> result = service.searchDestinations(null, filters, PageRequest.of(page, size));
        return ApiResponse.success(result);
    }

    @GetMapping("/destinations/{id}")
    public ApiResponse<Destination> getDestination(@PathVariable Long id) {
        return ApiResponse.success(service.getDestinationById(id));
    }

    @PostMapping("/destinations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Destination> createDestination(@RequestBody Destination dest) {
        return ApiResponse.success(service.createDestination(dest));
    }

    @PutMapping("/destinations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Destination> updateDestination(@PathVariable Long id, @RequestBody Destination dest) {
        return ApiResponse.success(service.updateDestination(id, dest));
    }

    @DeleteMapping("/destinations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDestination(@PathVariable Long id) {
        service.deleteDestination(id);
        return ApiResponse.success(null, "Deleted successfully");
    }

    @GetMapping("/services/{type}")
    public ApiResponse<Page<PublicTourismServiceDTO>> getServicesByType(
            @PathVariable TourismService.ServiceType type,
            @RequestParam(required = false) String location,
            Pageable pageable) {
        
        Page<TourismService> resultPage = service.getServicesByTypeAndLocation(type, location, pageable);
        Page<PublicTourismServiceDTO> dtoPage = resultPage.map(serviceMapper::toPublicDTO);
        return ApiResponse.success(dtoPage);
    }

    @GetMapping("/search")
    public ApiResponse<Page<Destination>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        filters.remove("q");
        filters.remove("sort");
        filters.remove("direction");
        filters.remove("page");
        filters.remove("size");

        Sort sorting = direction.equalsIgnoreCase("desc") ? Sort.by(sort).descending() : Sort.by(sort).ascending();
        
        return ApiResponse.success(service.searchDestinations(q, filters, PageRequest.of(page, size, sorting)));
    }

    @GetMapping("/internal/services/detail/{id}")
    public ApiResponse<InternalServiceResponse> getServiceDetail(@PathVariable Long id) {
        TourismService serviceEntity = service.getServiceById(id);
        return ApiResponse.success(serviceMapper.toInternalResponse(serviceEntity));
    }

    @PostMapping("/destinations/{id}/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PublicTourismServiceDTO> createService(
            @PathVariable Long id, 
            @RequestBody CreateServiceRequest request) {
        
        TourismService savedEntity = service.createService(id, request);
        
        return ApiResponse.success(serviceMapper.toPublicDTO(savedEntity));
    }

    @PostMapping(value = "/destinations/{id}/media", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> uploadDestinationMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {
        
        Media media = mediaService.addMediaToDestination(id, file, caption);
        return ApiResponse.success(mediaService.getFileUrl(media.getUrl()));
    }

    @PostMapping(value = "/services/{id}/media", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> uploadServiceMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {
        
        Media media = mediaService.addMediaToService(id, file, caption);
        return ApiResponse.success(mediaService.getFileUrl(media.getUrl()));
    }

    @PostMapping(value = "/destinations/{id}/media/batch", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<String>> uploadBatchDestinationMedia(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        
        List<String> urls = mediaService.addBatchMediaToDestination(id, files);
        return ApiResponse.success(urls);
    }

    @PostMapping(value = "/services/{id}/media/batch", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<String>> uploadBatchServiceMedia(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        
        List<String> urls = mediaService.addBatchMediaToService(id, files);
        return ApiResponse.success(urls);
    }

    @PostMapping("/services/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> setupInventory(@PathVariable Long id, 
                                            @RequestParam int total, 
                                            @RequestParam String start, 
                                            @RequestParam String end) {
        inventoryService.initInventory(id, total, LocalDate.parse(start), LocalDate.parse(end));
        return ApiResponse.success(null, "Inventory setup complete");
    }
    
    @PutMapping("/inventory/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Inventory> updateInventory(@PathVariable Long id, @RequestParam int newTotalStock) {
        Inventory updated = inventoryService.updateStockForDay(id, newTotalStock);
        return ApiResponse.success(updated);
    }
}