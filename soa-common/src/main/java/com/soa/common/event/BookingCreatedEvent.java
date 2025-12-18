package com.soa.common.event;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BookingCreatedEvent {
    private Long bookingId;
    private String userId;
    private Long serviceId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int quantity;
    private int guests;
    private BigDecimal amount;
    private String customerEmail;
}