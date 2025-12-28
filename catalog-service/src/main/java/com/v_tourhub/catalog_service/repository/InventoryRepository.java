package com.v_tourhub.catalog_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query(value = "UPDATE inventories i SET i.locked_stock = i.locked_stock + :qty " +
            "WHERE i.service_id = :serviceId AND i.date = :date " +
            "AND (i.total_stock - i.booked_stock - i.locked_stock) >= :qty", nativeQuery = true)
    int atomicLock(@Param("serviceId") Long serviceId, @Param("date") LocalDate date, @Param("qty") int qty);

    @Modifying
    @Query(value = "UPDATE inventories i SET i.locked_stock = i.locked_stock - :qty, i.booked_stock = i.booked_stock + :qty "
            +
            "WHERE i.service_id = :serviceId AND i.date = :date AND i.locked_stock >= :qty", nativeQuery = true)
    int atomicCommit(@Param("serviceId") Long serviceId, @Param("date") LocalDate date, @Param("qty") int qty);

    @Modifying
    @Query(value = "UPDATE inventories i SET i.locked_stock = i.locked_stock - :qty " +
            "WHERE i.service_id = :serviceId AND i.date = :date AND i.locked_stock >= :qty", nativeQuery = true)
    int atomicReleaseLocked(@Param("serviceId") Long serviceId, @Param("date") LocalDate date, @Param("qty") int qty);

    /**
     * Atomic Release (Booked): Trả lại kho từ trạng thái đã bán.
     * Dùng khi khách hàng hủy đơn đã thanh toán. Giảm booked_stock đi.
     */
    @Modifying
    @Query(value = "UPDATE inventories i SET i.booked_stock = i.booked_stock - :qty " +
            "WHERE i.service_id = :serviceId AND i.date = :date AND i.booked_stock >= :qty", nativeQuery = true)
    int atomicReleaseBooked(@Param("serviceId") Long serviceId, @Param("date") LocalDate date, @Param("qty") int qty);

    @Query("SELECT i FROM Inventory i WHERE i.service.id = :serviceId AND i.date BETWEEN :startDate AND :endDate")
    List<Inventory> findInventoryForRange(@Param("serviceId") Long serviceId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(i.totalStock - i.bookedStock - i.lockedStock) FROM Inventory i " +
            "WHERE i.service.id = :serviceId AND i.date >= :fromDate " +
            "AND (i.totalStock - i.bookedStock - i.lockedStock) > 0")
    Long countAvailableFutureStock(@Param("serviceId") Long serviceId, @Param("fromDate") LocalDate fromDate);
}
