package com.v_tourhub.catalog_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.v_tourhub.catalog_service.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Tìm inventory của 1 service trong 1 ngày cụ thể
    Optional<Inventory> findByServiceIdAndDate(Long serviceId, LocalDate date);

    // Tìm inventory của 1 service trong một danh sách các ngày
    // Dùng cho logic lock/commit/release
    @Query("SELECT i FROM Inventory i WHERE i.service.id = :serviceId AND i.date IN :dates")
    List<Inventory> findByServiceIdAndDatesIn(@Param("serviceId") Long serviceId, 
                                             @Param("dates") List<LocalDate> dates);
}
