package com.soa.common.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {
    private Long bookingId;
    private String userId;
    private Long serviceId;
    private String serviceName;
    
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int quantity;
    private int guests; 

    private String customerEmail;
    private String customerName;

    private BigDecimal totalAmount; 
    private String currency; 
}