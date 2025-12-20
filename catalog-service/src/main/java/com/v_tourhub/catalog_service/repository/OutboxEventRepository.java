package com.v_tourhub.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.v_tourhub.catalog_service.entity.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByIsPublishedFalseOrderByCreatedAtAsc();
}