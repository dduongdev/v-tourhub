package com.v_tourhub.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.v_tourhub.catalog_service.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
    
}
