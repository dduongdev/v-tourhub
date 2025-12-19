package com.soa.common.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {
    private Long bookingId;
    private String userId;
    private Long serviceId;
    private String serviceName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int quantity;
    private int guests;
    private BigDecimal amount;
    private String customerEmail;
    private LocalDateTime createdAt; 
}