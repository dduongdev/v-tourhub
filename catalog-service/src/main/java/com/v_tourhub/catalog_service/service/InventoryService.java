package com.v_tourhub.catalog_service.service;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingConfirmedEvent;
import com.soa.common.event.BookingCreatedEvent;
import com.soa.common.event.InventoryLockFailedEvent;
import com.soa.common.event.InventoryLockSuccessfulEvent;
import com.soa.common.exception.BusinessException;
import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.catalog_service.config.RabbitMQConfig;
import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.InventoryReservation;
import com.v_tourhub.catalog_service.entity.OutboxEvent;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.repository.InventoryRepository;
import com.v_tourhub.catalog_service.repository.InventoryReservationRepository;
import com.v_tourhub.catalog_service.repository.TourismServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final TourismServiceRepository serviceRepo;
    private final RabbitTemplate rabbitTemplate;
    private final InventoryReservationRepository reservationRepo;
    private final EventPublisherService eventPublisherService;

    // Constants
    private static final String BOOKING_EXCHANGE = "booking.exchange";
    private static final String ROUTING_KEY_LOCK_FAILED = "inventory.lock.failed";

    /**
     * 1. INIT INVENTORY: Khởi tạo kho cho 1 service
     */
    @Transactional
    public void initInventory(Long serviceId, int totalStock, LocalDate startDate, LocalDate endDate) {
        if (totalStock <= 0) {
            throw new IllegalArgumentException("Total stock must be greater than 0");
        }

        TourismService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        // 1️⃣ Build danh sách ngày
        List<LocalDate> dates = new ArrayList<>();
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            dates.add(d);
            d = d.plusDays(1);
        }

        // 2️⃣ Load toàn bộ inventory trong range (1 QUERY)
        List<Inventory> existingInventories = inventoryRepo.findByServiceIdAndDatesIn(serviceId, dates);

        Map<LocalDate, Inventory> inventoryMap = existingInventories.stream()
                .collect(Collectors.toMap(Inventory::getDate, inv -> inv));

        List<Inventory> toSave = new ArrayList<>();

        // 3️⃣ Upsert từng ngày
        for (LocalDate date : dates) {

            Inventory inv = inventoryMap.get(date);

            if (inv == null) {
                // 👉 INIT
                Inventory newInv = new Inventory();
                newInv.setService(service);
                newInv.setDate(date);
                newInv.setTotalStock(totalStock);
                newInv.setBookedStock(0);
                newInv.setLockedStock(0);
                toSave.add(newInv);
            } else {
                // 👉 UPDATE
                int usedStock = inv.getBookedStock() + inv.getLockedStock();

                if (usedStock > totalStock) {
                    throw new BusinessException(
                            String.format(
                                    "Cannot reduce total stock on %s. Used=%d, NewTotal=%d",
                                    date, usedStock, totalStock));
                }

                inv.setTotalStock(totalStock);
                toSave.add(inv);
            }
        }

        // 4️⃣ Save batch
        inventoryRepo.saveAll(toSave);

        log.info(
                "Upserted inventory for Service ID {}, dates {} -> {}, totalStock={}",
                serviceId, startDate, endDate, totalStock);
    }

    /**
     * 2. LOCK INVENTORY: Lắng nghe BookingCreatedEvent
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockInventory(BookingCreatedEvent event) {
        try {
            // 1. Check Idempotency: Nếu bookingId này đã được lock rồi thì bỏ qua
            if (reservationRepo.existsByBookingId(event.getBookingId())) {
                log.warn("Booking {} already has a reservation. Skipping lock.", event.getBookingId());
                return;
            }

            List<LocalDate> dates = getDatesForServiceType(null, event.getCheckIn(), event.getCheckOut());

            for (LocalDate date : dates) {
                int rowsAffected = inventoryRepo.atomicLock(event.getServiceId(), date, event.getQuantity());

                if (rowsAffected == 0) {
                    throw new BusinessException("Hết phòng/vé vào ngày " + date);
                }

                InventoryReservation res = InventoryReservation.builder()
                        .bookingId(event.getBookingId())
                        .serviceId(event.getServiceId())
                        .date(date)
                        .quantity(event.getQuantity())
                        .status(InventoryReservation.ReservationStatus.LOCKED)
                        .build();
                reservationRepo.save(res);

                InventoryLockSuccessfulEvent successEvent = InventoryLockSuccessfulEvent.builder()
                        .bookingId(event.getBookingId())
                        .build();

                eventPublisherService.saveEventToOutbox(
                        "Inventory",
                        event.getBookingId().toString(),
                        RabbitMQConfig.ROUTING_KEY_INVENTORY_LOCK_SUCCESSFUL,
                        successEvent);
            }
            log.info("Successfully atomic-locked inventory for Booking {}", event.getBookingId());

        } catch (Exception e) {
            log.error("Lock failed for Booking {}: {}", event.getBookingId(), e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            InventoryLockFailedEvent lockFailedEvent = InventoryLockFailedEvent.builder()
                    .bookingId(event.getBookingId())
                    .reason(e.getMessage())
                    .build();
            rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, ROUTING_KEY_LOCK_FAILED, lockFailedEvent);
        }
    }

    /**
     * 3. COMMIT INVENTORY: Lắng nghe BookingConfirmedEvent
     */
    @Transactional
    public void commitInventory(BookingConfirmedEvent event) {
        // Tìm các bản ghi đang LOCKED của booking này
        List<InventoryReservation> reservations = reservationRepo
                .findByBookingIdAndStatus(event.getBookingId(), InventoryReservation.ReservationStatus.LOCKED);

        if (reservations.isEmpty()) {
            log.warn("No LOCKED reservations found for booking {}. Maybe already committed?", event.getBookingId());
            return;
        }

        for (InventoryReservation res : reservations) {
            // 1. Cập nhật con số tổng hợp trong bảng Inventory (Atomic)
            inventoryRepo.atomicCommit(res.getServiceId(), res.getDate(), res.getQuantity());

            // 2. Cập nhật trạng thái bản ghi Reservation
            res.setStatus(InventoryReservation.ReservationStatus.COMMITTED);
        }
        reservationRepo.saveAll(reservations);
        log.info("Committed inventory for Booking {}", event.getBookingId());
    }

    /**
     * 4. RELEASE INVENTORY: Lắng nghe BookingCancelledEvent
     */
    @Transactional
    public void releaseInventory(BookingCancelledEvent event) {
        // 1. Tìm tất cả các bản ghi giữ chỗ của booking này
        List<InventoryReservation> reservations = reservationRepo.findByBookingId(event.getBookingId());

        if (reservations.isEmpty()) {
            log.warn(
                    "No inventory reservations found to release for cancelled Booking ID {}. Possibly already released or failed at lock stage.",
                    event.getBookingId());
            return;
        }

        // 2. Duyệt qua từng bản ghi và xử lý
        for (InventoryReservation res : reservations) {

            // Idempotency: Chỉ xử lý nếu trạng thái chưa phải là CANCELLED
            if (res.getStatus() == InventoryReservation.ReservationStatus.CANCELLED) {
                continue; // Bỏ qua, đã xử lý rồi
            }

            // 3. Dựa vào trạng thái trước đó để quyết định nhả kho nào
            if ("PENDING_PAYMENT".equals(event.getPreviousStatus())) {
                // Nhả kho từ locked_stock
                inventoryRepo.atomicReleaseLocked(res.getServiceId(), res.getDate(), res.getQuantity());
                log.info("Released locked stock for Booking ID {}, Date {}", event.getBookingId(), res.getDate());

            } else if ("CONFIRMED".equals(event.getPreviousStatus())) {
                // Nhả kho từ booked_stock
                inventoryRepo.atomicReleaseBooked(res.getServiceId(), res.getDate(), res.getQuantity());
                log.info("Released booked stock for Booking ID {}, Date {}", event.getBookingId(), res.getDate());

            } else {
                log.warn("Cannot determine how to release inventory for Booking ID {} with previous status '{}'",
                        event.getBookingId(), event.getPreviousStatus());
            }

            // 4. Cập nhật trạng thái của "biên lai" giữ chỗ
            res.setStatus(InventoryReservation.ReservationStatus.CANCELLED);
        }

        // 5. Lưu lại trạng thái mới của các "biên lai"
        reservationRepo.saveAll(reservations);
    }

    @Transactional
    public Inventory updateStockForDay(Long inventoryId, int newTotalStock) {
        Inventory inv = inventoryRepo.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", inventoryId));

        // Validate: Không được set total < (booked + locked)
        if (newTotalStock < (inv.getBookedStock() + inv.getLockedStock())) {
            throw new BusinessException("Không thể giảm tổng số lượng thấp hơn số đã bán hoặc đang giữ chỗ.");
        }

        inv.setTotalStock(newTotalStock);
        return inventoryRepo.save(inv);
    }

    /**
     * Helper method để xác định danh sách các ngày cần thao tác inventory
     * dựa trên loại dịch vụ.
     * Đây là "business rule engine" cho việc tính toán ngày tồn kho.
     * 
     * @param type  Loại dịch vụ (HOTEL, TOUR, ACTIVITY)
     * @param start Ngày bắt đầu
     * @param end   Ngày kết thúc
     * @return Danh sách các ngày cần kiểm tra/cập nhật kho.
     * @throws BusinessException nếu ngày tháng không hợp lệ.
     */
    private List<LocalDate> getDatesForServiceType(TourismService.ServiceType type, LocalDate start, LocalDate end) {
        // --- Validation đầu vào ---
        if (start == null) {
            throw new IllegalArgumentException("Start date cannot be null.");
        }
        if (type == null) {
            // Nếu không rõ type, mặc định xử lý như vé 1 ngày (an toàn nhất)
            log.warn("ServiceType is null, defaulting to single-day logic for date {}", start);
            return List.of(start);
        }

        // --- Logic theo từng loại dịch vụ ---
        switch (type) {
            case HOTEL:
                if (end == null) {
                    throw new BusinessException("End date is required for HOTEL bookings.");
                }
                if (!start.isBefore(end)) {
                    throw new BusinessException("Start date must be before end date for HOTEL bookings.");
                }

                // Logic khách sạn: Tính theo đêm. Ở từ ngày start đến TRƯỚC ngày end.
                // Ví dụ: check-in 24, check-out 26 -> ở đêm 24, đêm 25.
                long nights = ChronoUnit.DAYS.between(start, end);
                if (nights <= 0) {
                    throw new BusinessException("Booking duration must be at least 1 night for HOTEL.");
                }

                List<LocalDate> hotelDates = new ArrayList<>();
                for (int i = 0; i < nights; i++) {
                    hotelDates.add(start.plusDays(i));
                }
                return hotelDates;

            case TOUR:
            case ACTIVITY:
            case RESTAURANT: // Nhà hàng cũng tính theo 1 ngày
                // Logic Tour/Vé/Hoạt động: Chỉ tính vào ngày bắt đầu (ngày diễn ra).
                // Dù tour kéo dài 3 ngày, "kho" (số ghế) chỉ bị chiếm vào ngày khởi hành.
                return List.of(start);

            default:
                // Fallback an toàn cho các loại hình dịch vụ chưa xác định trong tương lai.
                log.warn("Unhandled ServiceType '{}', defaulting to single-day logic.", type);
                return List.of(start);
        }
    }
}