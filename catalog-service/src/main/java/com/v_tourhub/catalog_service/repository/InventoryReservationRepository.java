package com.v_tourhub.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.v_tourhub.catalog_service.entity.InventoryReservation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    /**
     * Kiểm tra xem một booking đã có bản ghi giữ chỗ nào chưa.
     * Dùng cho việc đảm bảo Idempotency (chống xử lý trùng lặp).
     * 
     * @param bookingId ID của booking
     * @return true nếu đã tồn tại, false nếu chưa.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Tìm tất cả các bản ghi giữ chỗ của một booking.
     * Dùng khi cần commit hoặc release kho.
     * 
     * @param bookingId ID của booking
     * @return Danh sách các bản ghi giữ chỗ (có thể rỗng).
     */
    List<InventoryReservation> findByBookingId(Long bookingId);

    /**
     * Tìm các bản ghi giữ chỗ của một booking với một trạng thái cụ thể.
     * Dùng để đảm bảo chỉ commit các bản ghi đang ở trạng thái LOCKED.
     * 
     * @param bookingId ID của booking
     * @param status    Trạng thái cần tìm
     * @return Danh sách các bản ghi giữ chỗ.
     */
    List<InventoryReservation> findByBookingIdAndStatus(Long bookingId, InventoryReservation.ReservationStatus status);

    /**
     * Tìm các bản ghi giữ chỗ với trạng thái cụ thể và được tạo trước một thời
     * điểm.
     * Dùng cho cleanup scheduler để giải phóng khóa cũ.
     * 
     * @param status    Trạng thái cần tìm
     * @param threshold Thời điểm ngưỡng
     * @return Danh sách các bản ghi giữ chỗ cũ.
     */
    List<InventoryReservation> findByStatusAndCreatedAtBefore(
            InventoryReservation.ReservationStatus status,
            LocalDateTime threshold);
}