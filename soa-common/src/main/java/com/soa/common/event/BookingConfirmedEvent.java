package com.soa.common.event;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class BookingConfirmedEvent {
    private Long serviceId;
    private Long bookingId;
    private String customerEmail;
    private String customerName;
    private String serviceName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int quantity;
}