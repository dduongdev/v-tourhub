package com.v_tourhub.booking_service.service;

import com.soa.common.dto.ApiResponse;
import com.soa.common.exception.BusinessException;
import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.booking_service.client.CatalogClient;
import com.v_tourhub.booking_service.config.RabbitMQConfig;
import com.v_tourhub.booking_service.dto.BookingResponse;
import com.v_tourhub.booking_service.dto.CatalogServiceDto;
import com.v_tourhub.booking_service.dto.CreateBookingRequest;
import com.v_tourhub.booking_service.entity.Booking;
import com.v_tourhub.booking_service.entity.BookingStatus;
import com.v_tourhub.booking_service.repository.BookingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepo;
    private final CatalogClient catalogClient;
    private final RabbitTemplate rabbitTemplate;
    private final RedisLockService redisLockService;

    private static final String BOOKING_EXCHANGE = "booking.exchange";
    private static final String ROUTING_KEY_CREATED = "booking.created";
    private static final String ROUTING_KEY_CANCELLED = "booking.cancelled";

    @Transactional
    public BookingResponse createBooking(String userId, CreateBookingRequest request) {
        String lockKey = "lock:inventory:" + request.getServiceId();

        if (!redisLockService.tryLock(lockKey, 10)) {
            throw new RuntimeException("Hệ thống đang bận xử lý dịch vụ này, vui lòng thử lại sau.");
        }

        try {
            ApiResponse<CatalogServiceDto> catalogResponse = catalogClient.getServiceDetail(request.getServiceId());

            if (catalogResponse == null || catalogResponse.getData() == null) {
                throw new ResourceNotFoundException("Dịch vụ không tồn tại hoặc Catalog Service đang gặp sự cố.");
            }

            CatalogServiceDto serviceInfo = catalogResponse.getData();

            if (Boolean.FALSE.equals(serviceInfo.getAvailability())) {
                throw new BusinessException("Dịch vụ này hiện đang tạm ngưng phục vụ.");
            }
            if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
                throw new BusinessException("Ngày Check-in phải trước ngày Check-out.");
            }

            BigDecimal totalPrice = serviceInfo.getPrice().multiply(new BigDecimal(request.getGuests()));

            Booking booking = Booking.builder()
                    .userId(userId)
                    .serviceId(serviceInfo.getId())
                    .serviceName(serviceInfo.getName())
                    .providerId(serviceInfo.getProviderId())
                    .status(BookingStatus.PENDING_PAYMENT)
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .guests(request.getGuests())
                    .totalPrice(totalPrice)
                    .inventoryLockToken(UUID.randomUUID().toString())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .customerName(request.getCustomerName())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .build();

            Booking savedBooking = bookingRepo.save(booking);

            Map<String, Object> event = new HashMap<>();
            event.put("bookingId", savedBooking.getId());
            event.put("userId", userId);
            event.put("amount", totalPrice);
            event.put("serviceName", serviceInfo.getName());
            event.put("customerEmail", request.getCustomerEmail());

            rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, ROUTING_KEY_CREATED, event);
            log.info("Booking created: ID={}, Status={}", savedBooking.getId(), savedBooking.getStatus());

            return mapToDto(savedBooking, serviceInfo.getName());

        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String userId) {
        List<Booking> bookings = bookingRepo.findByUserIdOrderByCreatedAtDesc(userId);

        return bookings.stream()
                .map(b -> mapToDto(b, "Service ID: " + b.getServiceId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId, String userId) {
        Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt hàng hoặc bạn không có quyền hủy."));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã bị hủy trước đó.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Không thể hủy đơn hàng đã hoàn thành.");
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        log.info("Booking cancelled: ID={}, OldStatus={}", bookingId, oldStatus);

        Map<String, Object> event = new HashMap<>();
        event.put("bookingId", bookingId);
        event.put("serviceId", booking.getServiceId());
        event.put("userId", userId);
        event.put("reason", "User requested cancellation");
        event.put("previousStatus", oldStatus.name());

        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, ROUTING_KEY_CANCELLED, event);
    }

    private BookingResponse mapToDto(Booking entity, String serviceName) {
        return BookingResponse.builder()
                .bookingId(entity.getId())
                .status(entity.getStatus().name())
                .serviceName(serviceName)
                .totalPrice(entity.getTotalPrice())
                .expiresAt(entity.getExpiresAt())
                .paymentUrl(entity.getStatus() == BookingStatus.PENDING_PAYMENT
                        ? "/api/payments/pay/" + entity.getId()
                        : null)
                .build();
    }

    // =========================================================================
    // 4. SAGA STEP: PAYMENT SUCCESS
    // =========================================================================
    @Transactional
    public void completeBooking(Long bookingId, String transactionId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CONFIRMED);
            
            log.info("Booking {} CONFIRMED. Transaction: {}", bookingId, transactionId);
            
            bookingRepo.save(booking);
            
            publishConfirmedEvent(booking);
        } else {
            log.warn("Ignored payment completion for Booking {} because status is {}", bookingId, booking.getStatus());
        }
    }

     private void publishConfirmedEvent(Booking booking) {
        Map<String, Object> event = new HashMap<>();
        event.put("bookingId", booking.getId());
        event.put("userId", booking.getUserId());
        event.put("serviceId", booking.getServiceId());
        event.put("providerId", booking.getProviderId());
        event.put("checkIn", booking.getCheckInDate().toString());
        event.put("checkOut", booking.getCheckOutDate().toString());
        event.put("guests", booking.getGuests());
        event.put("serviceName", booking.getServiceName());
        
        event.put("customerEmail", booking.getCustomerEmail());
        event.put("customerName", booking.getCustomerName());
        
        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, RabbitMQConfig.ROUTING_KEY_CONFIRMED, event);
        log.info("Published booking.confirmed event for Booking {}", booking.getId());
    }

    // =========================================================================
    // 5. SAGA STEP: PAYMENT FAILED (Compensating Transaction)
    // =========================================================================
    @Transactional
    public void handlePaymentFailure(Long bookingId, String reason) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            log.warn("Payment FAILED for Booking {}. Reason: {}. Cancelling booking...", bookingId, reason);
            
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(booking);

            Map<String, Object> event = new HashMap<>();
            event.put("bookingId", bookingId);
            event.put("serviceId", booking.getServiceId());
            event.put("reason", "Payment Failed: " + reason);
            event.put("inventoryLockToken", booking.getInventoryLockToken()); 
            
            rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, ROUTING_KEY_CANCELLED, event);
            log.info("Sent booking.cancelled event for Booking {}", bookingId);
        } else {
            log.info("Booking {} payment failed but status is already {}. Skipping.", bookingId, booking.getStatus());
        }
    }
}