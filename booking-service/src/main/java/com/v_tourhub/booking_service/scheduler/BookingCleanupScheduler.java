package com.v_tourhub.booking_service.scheduler;

import com.soa.common.event.BookingCancelledEvent;
import com.v_tourhub.booking_service.entity.Booking;
import com.v_tourhub.booking_service.entity.BookingStatus;
import com.v_tourhub.booking_service.repository.BookingRepository;
import com.v_tourhub.booking_service.service.EventPublisherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to automatically cancel expired bookings that are still in
 * PENDING_PAYMENT status.
 * Runs every 1 minute to check for bookings past their expiresAt timestamp.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepo;
    private final EventPublisherService eventPublisher;

    private static final String ROUTING_KEY_CANCELLED = "booking.cancelled";

    /**
     * Cleanup expired bookings every 1 minute
     */
    @Scheduled(fixedDelay = 60000) // 60 seconds = 1 minute
    @Transactional
    public void cleanupExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();

        // Find all PENDING_PAYMENT bookings that have expired
        List<Booking> expiredBookings = bookingRepo
                .findByStatusAndExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now);

        if (expiredBookings.isEmpty()) {
            return; // No expired bookings, skip logging
        }

        log.info("Found {} expired bookings to clean up", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // Update booking status
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancellationReason("Payment timeout - auto-cancelled");
                booking.setCancelledAt(now);
                bookingRepo.save(booking);

                // Publish BookingCancelledEvent to release inventory
                BookingCancelledEvent event = BookingCancelledEvent.builder()
                        .bookingId(booking.getId())
                        .serviceId(booking.getServiceId())
                        .userId(booking.getUserId())
                        .reason("Payment timeout - auto-cancelled")
                        .previousStatus("PENDING_PAYMENT")
                        .checkIn(booking.getCheckInDate())
                        .checkOut(booking.getCheckOutDate())
                        .quantity(booking.getQuantity())
                        .customerEmail(booking.getCustomerEmail())
                        .serviceName(booking.getServiceName())
                        .build();

                eventPublisher.saveEventToOutbox("Booking", booking.getId().toString(),
                        ROUTING_KEY_CANCELLED, event);

                log.info("Auto-cancelled expired booking ID: {} (expired at: {})",
                        booking.getId(), booking.getExpiresAt());

            } catch (Exception e) {
                log.error("Error cancelling expired booking ID: {}", booking.getId(), e);
                // Continue processing other bookings
            }
        }

        log.info("Successfully processed {} expired bookings", expiredBookings.size());
    }
}
