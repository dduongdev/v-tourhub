package com.v_tourhub.booking_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.v_tourhub.booking_service.entity.Booking;
import com.v_tourhub.booking_service.entity.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Booking> findByIdAndUserId(Long id, String userId);

    /**
     * Cập nhật trạng thái của booking từ INITIATED sang PENDING_PAYMENT một cách
     * nguyên tử.
     * Chỉ cập nhật nếu trạng thái hiện tại là INITIATED.
     * 
     * @param bookingId ID của booking cần cập nhật.
     * @param newStatus Trạng thái mới (PENDING_PAYMENT).
     * @param oldStatus Trạng thái cũ điều kiện (INITIATED).
     * @return Số dòng bị ảnh hưởng (1 nếu thành công, 0 nếu không khớp điều kiện).
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus WHERE b.id = :bookingId AND b.status = :oldStatus")
    int updateStatusIfCurrentStatusIs(
            @Param("bookingId") Long bookingId,
            @Param("newStatus") BookingStatus newStatus,
            @Param("oldStatus") BookingStatus oldStatus);

    List<Booking> findAllByOrderByCreatedAtDesc();

    // Query for expired bookings cleanup
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime expiresAt);
}