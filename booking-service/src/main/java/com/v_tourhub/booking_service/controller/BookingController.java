package com.v_tourhub.booking_service.controller;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.booking_service.dto.BookingResponse;
import com.v_tourhub.booking_service.dto.CreateBookingRequest;
import com.v_tourhub.booking_service.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // 1. Tạo Booking
    @PostMapping
    public ApiResponse<BookingResponse> createBooking(
            @RequestHeader(value = "X-User-Id", required = true) String userId, 
            @RequestBody @Valid CreateBookingRequest request) {
        return ApiResponse.success(bookingService.createBooking(userId, request));
    }

    // 2. Xem danh sách Booking của mình
    @GetMapping("/my-bookings")
    public ApiResponse<List<BookingResponse>> getMyBookings(
            @RequestHeader(value = "X-User-Id", required = true) String userId) {
        return ApiResponse.success(bookingService.getUserBookings(userId));
    }

    // 3. Hủy Booking
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelBooking(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = true) String userId) {
        bookingService.cancelBooking(id, userId);
        return ApiResponse.success(null, "Hủy đặt chỗ thành công. Nếu bạn đã thanh toán, tiền sẽ được hoàn lại trong 24h.");
    }
}