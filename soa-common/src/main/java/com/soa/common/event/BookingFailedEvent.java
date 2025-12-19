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
public class BookingFailedEvent {
    private Long bookingId;
    private String userId;
    private Long serviceId;
    private String reason; 
    
    private String customerEmail;
    private String serviceName;
    private LocalDate checkIn;
}