package com.v_tourhub.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.v_tourhub.catalog_service.entity.Attribute;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    
}
