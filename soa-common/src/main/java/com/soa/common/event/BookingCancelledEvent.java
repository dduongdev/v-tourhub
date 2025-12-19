package com.soa.common.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent {
    private Long bookingId;
    private Long serviceId;
    private String userId; 
    
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int quantity;

    private String reason;
    private String previousStatus;
}