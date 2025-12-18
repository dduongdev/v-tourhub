package com.v_tourhub.catalog_service.controller;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.catalog_service.dto.CreateServiceRequest;
import com.v_tourhub.catalog_service.dto.TourismServiceResponse;
import com.v_tourhub.catalog_service.entity.Category;
import com.v_tourhub.catalog_service.entity.Destination;
import com.v_tourhub.catalog_service.entity.Media;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.mapper.ServiceMapper;
import com.v_tourhub.catalog_service.service.CatalogService;
import com.v_tourhub.catalog_service.service.MediaService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService service;
    private final ServiceMapper serviceMapper;
    private final MediaService mediaService;

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

    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    @PostMapping("/destinations")
    public ApiResponse<Destination> createDestination(@RequestBody Destination dest) {
        return ApiResponse.success(service.createDestination(dest));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    @PutMapping("/destinations/{id}")
    public ApiResponse<Destination> updateDestination(@PathVariable Long id, @RequestBody Destination dest) {
        return ApiResponse.success(service.updateDestination(id, dest));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/destinations/{id}")
    public ApiResponse<Void> deleteDestination(@PathVariable Long id) {
        service.deleteDestination(id);
        return ApiResponse.success(null, "Deleted successfully");
    }

    @GetMapping("/services/{type}")
    public ApiResponse<Page<TourismServiceResponse>> getServicesByType(
            @PathVariable TourismService.ServiceType type,
            @RequestParam(required = false) String location,
            Pageable pageable) {
        
        Page<TourismService> resultPage = service.getServicesByTypeAndLocation(type, location, pageable);
        Page<TourismServiceResponse> dtoPage = resultPage.map(serviceMapper::toResponse);
        return ApiResponse.success(dtoPage);
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> getCategories() {
        return ApiResponse.success(service.getAllCategories());
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

    @GetMapping("/services/detail/{id}")
    public ApiResponse<TourismServiceResponse> getServiceDetail(@PathVariable Long id) {
        TourismService serviceEntity = service.getServiceById(id);
        return ApiResponse.success(serviceMapper.toResponse(serviceEntity));
    }

    @PostMapping("/destinations/{id}/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')") 
    public ApiResponse<TourismServiceResponse> createService(
            @PathVariable Long id, 
            @RequestBody CreateServiceRequest request) {
        
        TourismService entity = serviceMapper.toEntity(request);
        
        TourismService savedEntity = service.createService(id, entity);
        
        return ApiResponse.success(serviceMapper.toResponse(savedEntity));
    }

    @PostMapping(value = "/destinations/{id}/media", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ApiResponse<String> uploadDestinationMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {
        
        Media media = mediaService.addMediaToDestination(id, file, caption);
        return ApiResponse.success(mediaService.getFileUrl(media.getUrl()));
    }

    @PostMapping(value = "/services/{id}/media", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ApiResponse<String> uploadServiceMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {
        
        Media media = mediaService.addMediaToService(id, file, caption);
        return ApiResponse.success(mediaService.getFileUrl(media.getUrl()));
    }

    @PostMapping(value = "/destinations/{id}/media/batch", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ApiResponse<List<String>> uploadBatchDestinationMedia(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        
        List<String> urls = mediaService.addBatchMediaToDestination(id, files);
        return ApiResponse.success(urls);
    }

    @PostMapping(value = "/services/{id}/media/batch", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ApiResponse<List<String>> uploadBatchServiceMedia(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        
        List<String> urls = mediaService.addBatchMediaToService(id, files);
        return ApiResponse.success(urls);
    }
}