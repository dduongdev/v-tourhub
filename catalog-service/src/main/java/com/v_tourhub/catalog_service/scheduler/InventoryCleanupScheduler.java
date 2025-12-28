package com.v_tourhub.catalog_service.scheduler;

import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.InventoryReservation;
import com.v_tourhub.catalog_service.repository.InventoryRepository;
import com.v_tourhub.catalog_service.repository.InventoryReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to clean up stale inventory locks that haven't been committed
 * or cancelled.
 * Locks older than 20 minutes are considered stale and will be released.
 * Runs every 5 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryCleanupScheduler {

    private final InventoryReservationRepository reservationRepo;
    private final InventoryRepository inventoryRepo;

    private static final int STALE_LOCK_THRESHOLD_MINUTES = 20;

    /**
     * Cleanup stale locks every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 300 seconds = 5 minutes
    @Transactional
    public void cleanupStaleLocks() {
        // Locks older than 20 minutes are considered stale
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_LOCK_THRESHOLD_MINUTES);

        List<InventoryReservation> staleLocks = reservationRepo
                .findByStatusAndCreatedAtBefore(
                        InventoryReservation.ReservationStatus.LOCKED,
                        threshold);

        if (staleLocks.isEmpty()) {
            return; // No stale locks, skip logging
        }

        log.info("Found {} stale inventory locks to clean up", staleLocks.size());

        for (InventoryReservation lock : staleLocks) {
            try {
                // Release the locked stock
                inventoryRepo.atomicReleaseLocked(
                        lock.getServiceId(),
                        lock.getDate(),
                        lock.getQuantity());

                // Mark reservation as cancelled
                lock.setStatus(InventoryReservation.ReservationStatus.CANCELLED);
                reservationRepo.save(lock);

                log.info("Released stale lock for booking {}, service {}, date {}, quantity {} (locked at: {})",
                        lock.getBookingId(), lock.getServiceId(), lock.getDate(),
                        lock.getQuantity(), lock.getCreatedAt());

            } catch (Exception e) {
                log.error("Error releasing stale lock for booking {}, date {}",
                        lock.getBookingId(), lock.getDate(), e);
                // Continue processing other locks
            }
        }

        log.info("Successfully processed {} stale inventory locks", staleLocks.size());
    }
}
