package com.v_tourhub.catalog_service.service;

import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.catalog_service.dto.CreateServiceRequest;
import com.v_tourhub.catalog_service.entity.Destination;
import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.Location;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.mapper.ServiceMapper;
import com.v_tourhub.catalog_service.repository.DestinationRepository;
import com.v_tourhub.catalog_service.repository.InventoryRepository;
import com.v_tourhub.catalog_service.repository.TourismServiceRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final ServiceMapper serviceMapper;
    private final InventoryRepository inventoryRepo;

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

    public List<Inventory> getInventoryForService(Long serviceId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusDays(30);

        return inventoryRepo.findInventoryForRange(serviceId, startDate, endDate);
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#destinationId")
    public TourismService createService(Long destinationId, CreateServiceRequest request) {
        // 1. Tìm Destination
        Destination dest = getDestinationById(destinationId);
        
        // 2. Convert DTO -> Entity (Mapper đã xử lý name, desc, attributes...)
        TourismService service = serviceMapper.toEntity(request);
        service.setDestination(dest);

        // 3. Lưu Service để lấy ID
        TourismService savedService = serviceRepo.save(service);

        // 4. [LOGIC MỚI] Khởi tạo kho dựa trên request
        if (request.getInventory() != null) {
            CreateServiceRequest.InventoryConfig invConfig = request.getInventory();
            inventoryService.initInventory(
                savedService.getId(), 
                invConfig.getTotalStock(), 
                invConfig.getStartDate(), 
                invConfig.getEndDate()
            );
        } else {
            inventoryService.initInventory(savedService.getId(), 10, LocalDate.now(), LocalDate.now().plusYears(1));
        }

        return savedService;
    }
}
