package com.soa.payment_service.controller;

import com.soa.common.dto.ApiResponse;
import com.soa.payment_service.entity.Payment;
import com.soa.payment_service.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/vnpay/url/{bookingId}")
    public ApiResponse<String> createVnPayUrl(@PathVariable Long bookingId, HttpServletRequest request) {
        String url = paymentService.createVnPayUrl(bookingId, request);
        return ApiResponse.success(url);
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));

        try {
            Payment payment = paymentService.processVnPayCallback(params);
            response.sendRedirect("https://google.com?status=success&orderId=" + payment.getBookingId());
        } catch (Exception e) {
            response.sendRedirect("https://google.com?status=failed");
        }
    }
}