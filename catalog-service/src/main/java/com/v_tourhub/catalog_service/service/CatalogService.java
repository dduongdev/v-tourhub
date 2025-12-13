package com.v_tourhub.catalog_service.service;

import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.catalog_service.entity.Category;
import com.v_tourhub.catalog_service.entity.Destination;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final DestinationRepository destRepo;
    private final TourismServiceRepository serviceRepo;
    private final CategoryRepository categoryRepo;

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
        existing.setStatus(destDetails.getStatus());
        existing.setCategory(destDetails.getCategory());

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
                if (filters.containsKey("categoryId")) {
                    predicates.add(cb.equal(root.get("category").get("id"),
                            Long.valueOf(filters.get("categoryId"))));
                }
                if (filters.containsKey("status")) {
                    predicates.add(cb.equal(root.get("status"),
                            Destination.DestinationStatus.valueOf(filters.get("status"))));
                }
                if (filters.containsKey("minRating")) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"),
                            Double.valueOf(filters.get("minRating"))));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public List<TourismService> getServicesByTypeAndLocation(TourismService.ServiceType type, String location) {
        if (!StringUtils.hasText(location)) {
            return serviceRepo.findByType(type);
        }
        return serviceRepo.findByTypeAndLocation(type, location);
    }

    @Cacheable(value = "categories")
    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#id")
    public void updateRating(Long id, Double newRating, Integer totalReviews) {
        Destination dest = destRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));

        dest.setAverageRating(newRating);
        dest.setTotalReviews(totalReviews);

        destRepo.save(dest);
    }

    public TourismService getServiceById(Long id) {
        return serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    @Transactional
    @CacheEvict(value = "destinations", key = "#destinationId") 
    public TourismService createService(Long destinationId, TourismService service) {
        Destination dest = destRepo.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + destinationId));
        
        service.setDestination(dest);
        
        return serviceRepo.save(service);
    }
}
