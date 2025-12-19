package com.v_tourhub.booking_service.controller;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.booking_service.dto.BookingResponse;
import com.v_tourhub.booking_service.dto.CreateBookingRequest;
import com.v_tourhub.booking_service.entity.Booking;
import com.v_tourhub.booking_service.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ApiResponse<BookingResponse> createBooking(
            @RequestHeader(value = "X-User-Id", required = true) String userId, 
            @RequestBody @Valid CreateBookingRequest request) {
        return ApiResponse.success(bookingService.createBooking(userId, request));
    }

    @GetMapping("/my-bookings")
    public ApiResponse<List<BookingResponse>> getMyBookings(
            @RequestHeader(value = "X-User-Id", required = true) String userId) {
        return ApiResponse.success(bookingService.getUserBookings(userId));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) { 
        
        String userId = jwt.getClaimAsString("sub");
        Collection<String> roles = getRoles(jwt);

        if (roles.contains("ROLE_ADMIN")) {
            bookingService.cancelBookingByAdmin(id);
        } else {
            bookingService.cancelBooking(id, userId);
        }
        
        return ApiResponse.success(null, "Hủy đặt chỗ thành công.");
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Booking> getBookingById(@PathVariable Long id) {
        return ApiResponse.success(bookingService.getBooking(id));
    }
    
    private Collection<String> getRoles(Jwt jwt) {
        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess == null || realmAccess.isEmpty()) return List.of();
        return ((List<String>) realmAccess.get("roles")).stream()
                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                .collect(Collectors.toList());
    }
}