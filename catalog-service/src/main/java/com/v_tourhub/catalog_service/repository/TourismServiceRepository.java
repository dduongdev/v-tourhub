package com.v_tourhub.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.v_tourhub.catalog_service.entity.TourismService;

import java.util.List;

@Repository
public interface TourismServiceRepository extends JpaRepository<TourismService, Long> {

    List<TourismService> findByType(TourismService.ServiceType type);

    List<TourismService> findByDestinationId(Long destinationId);

    @Query("SELECT s FROM TourismService s " +
           "WHERE s.type = :type " +
           "AND (:location IS NULL OR :location = '' OR " +
           "LOWER(s.destination.location.city) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
           "LOWER(s.destination.location.province) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<TourismService> findByTypeAndLocation(@Param("type") TourismService.ServiceType type,
                                               @Param("location") String location);
}
