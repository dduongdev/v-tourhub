package com.v_tourhub.catalog_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.v_tourhub.catalog_service.entity.Inventory;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Tìm inventory của 1 service trong 1 ngày cụ thể
    Optional<Inventory> findByServiceIdAndDate(Long serviceId, LocalDate date);

    // Tìm inventory của 1 service trong một danh sách các ngày
    // Dùng cho logic lock/commit/release
    @Query("SELECT i FROM Inventory i WHERE i.service.id = :serviceId AND i.date IN :dates")
    List<Inventory> findByServiceIdAndDatesIn(@Param("serviceId") Long serviceId, 
                                             @Param("dates") List<LocalDate> dates);

    /**
     * Tìm inventory trong một khoảng thời gian VÀ khóa các dòng kết quả để ghi (Pessimistic Write Lock).
     * Ngăn chặn các transaction khác đọc/ghi vào các dòng này cho đến khi transaction hiện tại hoàn tất.
     * Rất quan trọng để tránh race condition ở mức database.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.service.id = :serviceId AND i.date IN :dates")
    List<Inventory> findInventoryForRangeWithLock(@Param("serviceId") Long serviceId, 
                                                  @Param("dates") List<LocalDate> dates);
}
