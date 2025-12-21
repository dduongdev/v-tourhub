package com.v_tourhub.booking_service.service;

import com.soa.common.dto.ApiResponse;
import com.soa.common.dto.InternalServiceResponse;
import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingConfirmedEvent;
import com.soa.common.event.BookingCreatedEvent;
import com.soa.common.event.BookingFailedEvent;
import com.soa.common.event.BookingReadyForPaymentEvent;
import com.soa.common.event.InventoryLockFailedEvent;
import com.soa.common.exception.BusinessException;
import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.booking_service.client.CatalogClient;
import com.v_tourhub.booking_service.config.RabbitMQConfig;
import com.v_tourhub.booking_service.dto.BookingResponse;
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
import java.util.ArrayList;
import java.util.List;
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
    private final EventPublisherService eventPublisher;

    private static final String BOOKING_EXCHANGE = "booking.exchange";
    private static final String ROUTING_KEY_CREATED = "booking.created";
    private static final String ROUTING_KEY_CONFIRMED = "booking.confirmed";
    private static final String ROUTING_KEY_CANCELLED = "booking.cancelled";

    @Transactional
    public BookingResponse createBooking(String userId, CreateBookingRequest request) {
        String lockKey = "lock:inventory:" + request.getServiceId();

        if (!redisLockService.tryLock(lockKey, 10)) {
            throw new BusinessException("Hệ thống đang bận xử lý dịch vụ này, vui lòng thử lại sau.");
        }

        try {
            ApiResponse<InternalServiceResponse> catalogResponse = catalogClient
                    .getServiceDetail(request.getServiceId());
            if (catalogResponse == null || catalogResponse.getData() == null) {
                throw new ResourceNotFoundException("Dịch vụ", "id", request.getServiceId());
            }

            InternalServiceResponse serviceInfo = catalogResponse.getData();

            int quantityToLock = determineQuantity(request, serviceInfo.getType());

            validateBookingRequest(request, serviceInfo, quantityToLock);

            BigDecimal totalPrice = calculateTotalPrice(serviceInfo.getPrice(), quantityToLock, request.getGuests(),
                    serviceInfo.getType());

            Booking booking = Booking.builder()
                    .userId(userId)
                    .serviceId(serviceInfo.getId())
                    .serviceName(serviceInfo.getName())
                    .status(BookingStatus.INITIATED)
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .guests(request.getGuests())
                    .quantity(quantityToLock)
                    .totalPrice(totalPrice)
                    .inventoryLockToken(UUID.randomUUID().toString())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .customerName(request.getCustomerName())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .build();

            Booking savedBooking = bookingRepo.save(booking);

            BookingCreatedEvent event = BookingCreatedEvent.builder()
                    .bookingId(savedBooking.getId())
                    .userId(userId)
                    .serviceId(serviceInfo.getId())
                    .serviceName(serviceInfo.getName())
                    .checkIn(request.getCheckInDate())
                    .checkOut(request.getCheckOutDate())
                    .quantity(quantityToLock)
                    .guests(request.getGuests())
                    .amount(totalPrice)
                    .customerEmail(request.getCustomerEmail())
                    .createdAt(LocalDateTime.now())
                    .build();

            eventPublisher.saveEventToOutbox(
                    "Booking",
                    savedBooking.getId().toString(),
                    ROUTING_KEY_CREATED,
                    event);
            log.info("Published BookingCreatedEvent for ID={}", savedBooking.getId());

            return mapToDto(savedBooking, serviceInfo.getName());

        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    private int determineQuantity(CreateBookingRequest request, String serviceType) {
        // Ưu tiên quantity do user gửi lên
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            return request.getQuantity();
        }

        // Nếu không, tự suy luận
        if ("HOTEL".equalsIgnoreCase(serviceType) || "RESTAURANT".equalsIgnoreCase(serviceType)) {
            return 1; // Mặc định đặt 1 phòng / 1 bàn
        } else { // TOUR, ACTIVITY
            return request.getGuests(); // Mặc định 1 người 1 vé/suất
        }
    }

    private void validateBookingRequest(CreateBookingRequest request, InternalServiceResponse serviceInfo,
            int quantity) {
        if (Boolean.FALSE.equals(serviceInfo.getAvailability())) {
            throw new BusinessException("Dịch vụ này hiện đang tạm ngưng phục vụ.");
        }

        if ("HOTEL".equalsIgnoreCase(serviceInfo.getType())) {
            if (request.getCheckOutDate() == null || !request.getCheckInDate().isBefore(request.getCheckOutDate())) {
                throw new BusinessException("Ngày Check-out phải sau ngày Check-in đối với Khách sạn.");
            }
        }
    }

    private BigDecimal calculateTotalPrice(BigDecimal pricePerUnit, int quantity, int guests, String serviceType) {
        return pricePerUnit.multiply(new BigDecimal(quantity));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String userId) {
        List<Booking> bookings = bookingRepo.findByUserIdOrderByCreatedAtDesc(userId);

        List<BookingResponse> responses = new ArrayList<>();
        for (Booking booking : bookings) {
            ApiResponse<InternalServiceResponse> catalogResponse = catalogClient
                    .getServiceDetail(booking.getServiceId());
            String serviceName = (catalogResponse != null && catalogResponse.getData() != null)
                    ? catalogResponse.getData().getName()
                    : "Service ID: " + booking.getServiceId();

            responses.add(mapToDto(booking, serviceName));
        }

        return responses;
    }

    @Transactional
    public void cancelBooking(Long bookingId, String userId) {
        Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đặt hàng hoặc bạn không có quyền hủy."));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã bị hủy trước đó.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Không thể hủy đơn hàng đã hoàn thành.");
        }

        String oldStatus = booking.getStatus().name();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        log.info("Booking cancelled by user: ID={}, OldStatus={}", bookingId, oldStatus);

        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(bookingId)
                .serviceId(booking.getServiceId())
                .userId(userId)
                .reason("User requested cancellation")
                .previousStatus(oldStatus)
                .checkIn(booking.getCheckInDate())
                .checkOut(booking.getCheckOutDate())
                .quantity(booking.getQuantity())
                .customerEmail(booking.getCustomerEmail())
                .serviceName(booking.getServiceName())
                .build();

        eventPublisher.saveEventToOutbox(
                "Booking",
                booking.getId().toString(),
                ROUTING_KEY_CANCELLED,
                event);
    }

    private BookingResponse mapToDto(Booking entity, String serviceName) {
        boolean isPaymentReady = entity.getStatus() == BookingStatus.PENDING_PAYMENT;
        return BookingResponse.builder()
                .bookingId(entity.getId())
                .status(entity.getStatus().name())
                .serviceName(serviceName)
                .totalPrice(entity.getTotalPrice())
                .expiresAt(entity.getExpiresAt())
                .isPaymentReady(isPaymentReady)
                .paymentUrl(isPaymentReady ? "/api/payments/vnpay/url/" + entity.getId() : null)
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
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .serviceId(booking.getServiceId())
                .checkIn(booking.getCheckInDate())
                .checkOut(booking.getCheckOutDate())
                .guests(booking.getGuests())
                .quantity(booking.getQuantity())
                .customerEmail(booking.getCustomerEmail())
                .customerName(booking.getCustomerName())
                .serviceName(booking.getServiceName())
                .totalAmount(booking.getTotalPrice())
                .currency("VND")
                .build();

        eventPublisher.saveEventToOutbox(
                "Booking",
                booking.getId().toString(),
                ROUTING_KEY_CONFIRMED,
                event);
        log.info("Published BookingConfirmedEvent for Booking ID {}", booking.getId());
    }

    // =========================================================================
    // 5. SAGA STEP: PAYMENT FAILED (Compensating Transaction)
    // =========================================================================
    @Transactional
    public void handlePaymentFailure(Long bookingId, String reason) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            log.warn("Payment FAILED for Booking {}. Reason: {}. Cancelling...", bookingId, reason);

            String oldStatus = booking.getStatus().name();
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(booking);

            BookingCancelledEvent event = BookingCancelledEvent.builder()
                    .bookingId(bookingId)
                    .serviceId(booking.getServiceId())
                    .userId(booking.getUserId())
                    .reason("Payment Failed: " + reason)
                    .previousStatus(oldStatus)
                    .checkIn(booking.getCheckInDate())
                    .checkOut(booking.getCheckOutDate())
                    .quantity(booking.getQuantity())
                    .customerEmail(booking.getCustomerEmail())
                    .serviceName(booking.getServiceName())
                    .build();

            eventPublisher.saveEventToOutbox(
                    "Booking",
                    booking.getId().toString(),
                    ROUTING_KEY_CANCELLED,
                    event);
            log.info("Published BookingCancelledEvent for Booking ID {}", bookingId);
        } else {
            log.info("Payment failed but booking {} is already in status {}. Skipping.", bookingId,
                    booking.getStatus());
        }
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId) {
        log.info("Admin fetching booking with ID: {}", bookingId);
        return bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    @Transactional
    public void cancelBookingByAdmin(Long bookingId) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Admin tried to cancel an already cancelled booking: {}", bookingId);
            return;
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Không thể hủy đơn hàng đã hoàn thành.");
        }

        String oldStatus = booking.getStatus().name();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);
        log.info("Booking cancelled by ADMIN: ID={}, OldStatus={}", bookingId, oldStatus);

        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(bookingId)
                .serviceId(booking.getServiceId())
                .userId(booking.getUserId())
                .reason("Cancelled by Administrator")
                .previousStatus(oldStatus)
                .checkIn(booking.getCheckInDate())
                .checkOut(booking.getCheckOutDate())
                .quantity(booking.getQuantity())
                .customerEmail(booking.getCustomerEmail())
                .serviceName(booking.getServiceName())
                .build();

        eventPublisher.saveEventToOutbox(
                "Booking",
                booking.getId().toString(),
                ROUTING_KEY_CANCELLED,
                event);
    }

    @Transactional
    public void handleInventoryLockFailure(InventoryLockFailedEvent event) {
        Booking booking = bookingRepo.findById(event.getBookingId())
                .orElse(null);

        if (booking == null) {
            log.warn("Received inventory.lock.failed for a non-existent booking ID: {}", event.getBookingId());
            return;
        }

        if (booking.getStatus() == BookingStatus.INITIATED) {
            log.warn("Inventory lock FAILED for Booking {}. Reason: {}. Cancelling...", event.getBookingId(),
                    event.getReason());

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(booking);

            BookingFailedEvent failedEvent = BookingFailedEvent.builder()
                    .bookingId(booking.getId())
                    .userId(booking.getUserId())
                    .serviceId(booking.getServiceId())
                    .reason("Inventory Lock Failed: " + event.getReason())
                    .customerEmail(booking.getCustomerEmail())
                    .serviceName(booking.getServiceName())
                    .checkIn(booking.getCheckInDate())
                    .build();

            eventPublisher.saveEventToOutbox(
                    "Booking",
                    booking.getId().toString(),
                    RabbitMQConfig.ROUTING_KEY_BOOKING_FAILED,
                    failedEvent);
            log.info("Published BookingCancelledEvent due to inventory lock failure for Booking ID {}",
                    booking.getId());
        }
    }

    @Transactional
    public void moveToPendingPayment(Long bookingId) {
        int rowsAffected = bookingRepo.updateStatusIfCurrentStatusIs(
            bookingId, 
            BookingStatus.PENDING_PAYMENT, 
            BookingStatus.INITIATED
        );
        
        if (rowsAffected > 0) {
            log.info("Booking {} moved to PENDING_PAYMENT.", bookingId);
            Booking booking = getBooking(bookingId); 
            BookingReadyForPaymentEvent event = BookingReadyForPaymentEvent.builder()
                    .bookingId(bookingId)
                    .userId(booking.getUserId())
                    .amount(booking.getTotalPrice())
                    .currency("VND")
                    .customerEmail(booking.getCustomerEmail())
                    .expiresAt(booking.getExpiresAt())
                    .serviceName(booking.getServiceName())
                    .checkIn(booking.getCheckInDate())
                    .build();
            
            eventPublisher.saveEventToOutbox(
                "Booking",
                bookingId.toString(),
                RabbitMQConfig.ROUTING_KEY_READY_FOR_PAYMENT,
                event
            );
        } else {
            log.warn("Could not move Booking {} to PENDING_PAYMENT. It was not in INITIATED state.", bookingId);
        }
    }

    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepo.findAllByOrderByCreatedAtDesc();

        List<BookingResponse> responses = new ArrayList<>();
        for (Booking booking : bookings) {
            ApiResponse<InternalServiceResponse> catalogResponse = catalogClient
                    .getServiceDetail(booking.getServiceId());
            String serviceName = (catalogResponse != null && catalogResponse.getData() != null)
                    ? catalogResponse.getData().getName()
                    : "Service ID: " + booking.getServiceId();

            responses.add(mapToDto(booking, serviceName));
        }

        return responses;
    }
}