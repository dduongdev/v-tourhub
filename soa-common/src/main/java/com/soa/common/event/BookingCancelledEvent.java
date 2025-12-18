package com.soa.common.event;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class BookingCancelledEvent {
    private Long bookingId;
    private Long serviceId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int quantity;
    private String reason;
    private String previousStatus;
}