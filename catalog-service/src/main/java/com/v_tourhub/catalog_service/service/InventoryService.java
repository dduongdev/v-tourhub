package com.v_tourhub.catalog_service.service;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingConfirmedEvent;
import com.soa.common.event.BookingCreatedEvent;
import com.soa.common.event.InventoryLockFailedEvent;
import com.soa.common.exception.BusinessException;
import com.soa.common.exception.ResourceNotFoundException;
import com.v_tourhub.catalog_service.entity.Inventory;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.repository.InventoryRepository;
import com.v_tourhub.catalog_service.repository.TourismServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final TourismServiceRepository serviceRepo;
    private final RabbitTemplate rabbitTemplate;

    // Constants
    private static final String BOOKING_EXCHANGE = "booking.exchange";
    private static final String ROUTING_KEY_LOCK_FAILED = "inventory.lock.failed";

    /**
     * 1. INIT INVENTORY: Khởi tạo kho cho 1 service
     */
    @Transactional
    public void initInventory(Long serviceId, int totalStock, LocalDate startDate, LocalDate endDate) {
        TourismService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        List<Inventory> inventoriesToSave = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            final LocalDate finalCurrentDate = currentDate; // Cần final để dùng trong lambda

            // Chỉ tạo mới nếu ngày đó chưa có trong DB
            inventoryRepo.findByServiceIdAndDate(serviceId, currentDate).ifPresentOrElse(
                    (existingInv) -> {
                        /* Đã tồn tại, có thể log hoặc update totalStock nếu muốn */ },
                    () -> {
                        Inventory newInv = new Inventory();
                        newInv.setService(service);
                        newInv.setDate(finalCurrentDate);
                        newInv.setTotalStock(totalStock);
                        newInv.setBookedStock(0);
                        newInv.setLockedStock(0);
                        inventoriesToSave.add(newInv);
                    });
            currentDate = currentDate.plusDays(1);
        }

        if (!inventoriesToSave.isEmpty()) {
            inventoryRepo.saveAll(inventoriesToSave);
            log.info("Initialized {} inventory records for Service ID {}", inventoriesToSave.size(), serviceId);
        }
    }

    /**
     * 2. LOCK INVENTORY: Lắng nghe BookingCreatedEvent
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockInventory(BookingCreatedEvent event) {
        try {
            TourismService service = serviceRepo.findById(event.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", event.getServiceId()));

            List<LocalDate> datesToLock = getDatesForServiceType(
                    service.getType(), event.getCheckIn(), event.getCheckOut());

            List<Inventory> inventories = inventoryRepo.findByServiceIdAndDatesIn(event.getServiceId(), datesToLock);

            if (inventories.size() != datesToLock.size()) {
                throw new BusinessException("Lịch chưa được thiết lập đầy đủ cho các ngày bạn chọn.");
            }

            for (Inventory inv : inventories) {
                if (inv.getAvailableStock() < event.getQuantity()) {
                    throw new BusinessException("Hết phòng/vé vào ngày: " + inv.getDate());
                }
                inv.setLockedStock(inv.getLockedStock() + event.getQuantity());
            }

            inventoryRepo.saveAll(inventories);
            log.info("Locked inventory for Booking ID {}", event.getBookingId());

        } catch (Exception e) {
            log.error("Failed to lock inventory for Booking ID {}: {}", event.getBookingId(), e.getMessage());
            // Bắn event báo lỗi ngược lại cho Booking Service để hủy đơn
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
        List<LocalDate> datesToCommit = getDatesForServiceType(null, event.getCheckIn(), event.getCheckOut());
        List<Inventory> inventories = inventoryRepo.findByServiceIdAndDatesIn(event.getServiceId(), datesToCommit);

        for (Inventory inv : inventories) {
            if (inv.getLockedStock() >= event.getQuantity()) {
                inv.setLockedStock(inv.getLockedStock() - event.getQuantity());
                inv.setBookedStock(inv.getBookedStock() + event.getQuantity());
            } else {
                log.warn("Inventory Anomaly: Locked stock is less than quantity to commit for Service {} on {}",
                        event.getServiceId(), inv.getDate());
                inv.setBookedStock(inv.getBookedStock() + event.getQuantity());
            }
        }
        inventoryRepo.saveAll(inventories);
        log.info("Committed inventory for Booking ID {}", event.getBookingId());
    }

    /**
     * 4. RELEASE INVENTORY: Lắng nghe BookingCancelledEvent
     */
    @Transactional
    public void releaseInventory(BookingCancelledEvent event) {
        List<LocalDate> datesToRelease = getDatesForServiceType(null, event.getCheckIn(), event.getCheckOut());
        List<Inventory> inventories = inventoryRepo.findByServiceIdAndDatesIn(event.getServiceId(), datesToRelease);

        for (Inventory inv : inventories) {
            if ("PENDING_PAYMENT".equals(event.getPreviousStatus())) {
                if (inv.getLockedStock() >= event.getQuantity()) {
                    inv.setLockedStock(inv.getLockedStock() - event.getQuantity());
                }
            } else if ("CONFIRMED".equals(event.getPreviousStatus())) {
                if (inv.getBookedStock() >= event.getQuantity()) {
                    inv.setBookedStock(inv.getBookedStock() - event.getQuantity());
                }
            } else {
                log.warn("Cannot determine how to release inventory for booking {} with previous status {}",
                        event.getBookingId(), event.getPreviousStatus());
            }
        }
        inventoryRepo.saveAll(inventories);
        log.info("Released inventory for cancelled Booking ID {}", event.getBookingId());
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