package com.v_tourhub.catalog_service.service;

import com.soa.common.dto.ApiResponse;
import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.catalog_service.entity.Category;
import com.v_tourhub.catalog_service.entity.Destination;
import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.Location;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.repository.CategoryRepository;
import com.v_tourhub.catalog_service.repository.DestinationRepository;
import com.v_tourhub.catalog_service.repository.TourismServiceRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final DestinationRepository destRepo;
    private final TourismServiceRepository serviceRepo;
    private final InventoryService inventoryService;

    @Cacheable(value = "destinations", key = "#id")
    public Destination getDestinationById(Long id) {
        return destRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
    }

    @Transactional
    @CacheEvict(value = "destinations", allEntries = true)
    public Destination createDestination(Destination destination) {
        return destRepo.save(destination);
    }

    @Transactional
    @CachePut(value = "destinations", key = "#id")
    public Destination updateDestination(Long id, Destination destDetails) {
        Destination existing = getDestinationById(id);

        existing.setName(destDetails.getName());
        existing.setDescription(destDetails.getDescription());

        if (destDetails.getLocation() != null) {
            if (existing.getLocation() == null) {
                existing.setLocation(destDetails.getLocation());
            } else {
                Location l = existing.getLocation();
                Location newL = destDetails.getLocation();
                l.setAddress(newL.getAddress());
                l.setCity(newL.getCity());
                l.setProvince(newL.getProvince());
                l.setLatitude(newL.getLatitude());
                l.setLongitude(newL.getLongitude());
            }
        }

        return destRepo.save(existing);
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#id")
    public void deleteDestination(Long id) {
        if (!destRepo.existsById(id)) {
            throw new RuntimeException("Destination not found");
        }
        destRepo.deleteById(id);
    }

    public Page<Destination> searchDestinations(String q, Map<String, String> filters, Pageable pageable) {
        return destRepo.findAll((Specification<Destination>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(q)) {
                String likePattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)
                ));
            }

            if (filters != null) {
                if (filters.containsKey("city")) {
                    predicates.add(cb.like(cb.lower(root.get("location").get("city")),
                            "%" + filters.get("city").toLowerCase() + "%"));
                }
                if (filters.containsKey("minRating")) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"),
                            Double.valueOf(filters.get("minRating"))));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public Page<TourismService> getServicesByTypeAndLocation(TourismService.ServiceType type, String location, Pageable pageable) {
        if (!StringUtils.hasText(location)) {
            return serviceRepo.findByType(type, pageable);
        }
        return serviceRepo.findByTypeAndLocation(type, location, pageable);
    }

    public TourismService getServiceById(Long id) {
        return serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#destinationId") 
    public TourismService createService(Long destinationId, TourismService service) {
        Destination dest = getDestinationById(destinationId);
        service.setDestination(dest);

        // 2. Lưu service xuống DB để lấy ID
        TourismService savedService = serviceRepo.save(service);

        // 3. [QUAN TRỌNG] Gọi InventoryService để khởi tạo kho
        // Mặc định: Tạo kho 10 slot/phòng cho 1 năm tới.
        // Trong thực tế, có thể cần một API riêng để Admin cấu hình số lượng này.
        int defaultStock = 10;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        inventoryService.initInventory(savedService.getId(), defaultStock, startDate, endDate);

        return savedService;
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
    
    // (THÊM MỚI) Endpoint để cập nhật kho của một ngày cụ thể
    @PutMapping("/inventory/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Inventory> updateInventory(@PathVariable Long id, @RequestParam int newTotalStock) {
        Inventory updated = inventoryService.updateStockForDay(id, newTotalStock);
        return ApiResponse.success(updated);
    }
}
