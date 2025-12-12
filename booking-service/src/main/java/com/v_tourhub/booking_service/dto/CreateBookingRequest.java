package com.v_tourhub.booking_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Check-in date is required")
    @Future(message = "Check-in date must be in the future")
    private LocalDate checkInDate;

    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "Guests must be at least 1")
    private Integer guests;

    // Thông tin khách hàng (snapshot)
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}