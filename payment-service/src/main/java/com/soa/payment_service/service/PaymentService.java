package com.soa.payment_service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soa.common.event.BookingCancelledEvent;
import com.soa.common.event.BookingReadyForPaymentEvent;
import com.soa.common.event.PaymentCompletedEvent;
import com.soa.common.event.PaymentFailedEvent;
import com.soa.common.exception.BusinessException;
import com.soa.common.exception.ResourceNotFoundException;
import com.soa.payment_service.config.RabbitMQConfig;
import com.soa.payment_service.config.VNPayConfig;
import com.soa.payment_service.entity.Payment;
import com.soa.payment_service.entity.PaymentMethod;
import com.soa.payment_service.entity.PaymentStatus;
import com.soa.payment_service.repository.PaymentRepository;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final VNPayConfig vnpayConfig;
    private final EventPublisherService eventPublisher;

    public String createVnPayUrl(Long bookingId, HttpServletRequest request) {
        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new BusinessException(
                    "Payment is not ready or has been processed. Current status: " + payment.getStatus());
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Booking already paid");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment has been cancelled for this booking");
        }

        long amount = payment.getAmount().longValue() * 100;

        String vnp_TxnRef = String.valueOf(bookingId);
        String vnp_IpAddr = vnpayConfig.getIpAddress(request);
        String vnp_TmnCode = vnpayConfig.getVnp_TmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpayConfig.getVnp_Version());
        vnp_Params.put("vnp_Command", vnpayConfig.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", vnpayConfig.getOrderType());
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(vietnamZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnp_CreateDate = now.format(formatter);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        String vnp_ExpireDate = now.plusMinutes(15).format(formatter);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnpayConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnpayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Transactional
    public Payment processVnPayCallback(Map<String, String> queryParams) {
        String vnp_ResponseCode = queryParams.get("vnp_ResponseCode");
        String bookingId = queryParams.get("vnp_TxnRef");
        String vnp_SecureHash = queryParams.get("vnp_SecureHash");

        if (vnp_SecureHash == null || !verifySignature(queryParams, vnp_SecureHash)) {
            log.error("Checksum verification failed for booking {}", bookingId);
            handlePaymentFailed(Long.valueOf(bookingId), "Invalid Checksum");
            throw new BusinessException("Invalid Checksum");
        }

        Payment payment = paymentRepo.findByBookingId(Long.valueOf(bookingId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            log.warn("Payment for booking {} already processed. Current status: {}", bookingId, payment.getStatus());
            return payment;
        }

        if ("00".equals(vnp_ResponseCode)) {
            log.info("Payment SUCCESS for booking {}", bookingId);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setGatewayTransactionId(queryParams.get("vnp_TransactionNo"));
            payment.setPaidAt(LocalDateTime.now()); // ← SET PAID TIMESTAMP
            paymentRepo.save(payment);

            publishPaymentEvent(payment, RabbitMQConfig.ROUTING_KEY_PAYMENT_COMPLETED, "SUCCESS");

            return payment;
        } else {
            log.warn("Payment FAILED for booking {} with code {}", bookingId, vnp_ResponseCode);

            handlePaymentFailed(payment, "VNPay Response Code: " + vnp_ResponseCode);

            throw new RuntimeException("Payment Failed at Gateway");
        }
    }

    /**
     * Initiate refund for a completed payment
     * 
     * @param bookingId    - booking ID to refund
     * @param reason       - reason for refund
     * @param refundAmount - amount to refund (can be partial)
     */
    @Transactional
    public void initiateRefund(Long bookingId, String reason, BigDecimal refundAmount) {
        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));

        // Validation: Can only refund COMPLETED payments
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(
                    "Cannot refund a payment that is not completed. Current status: " + payment.getStatus());
        }

        // Validation: Refund amount cannot exceed original payment
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("Refund amount cannot exceed original payment amount");
        }

        // TODO: Call VNPay Refund API (placeholder for now)
        // In production, you would call VNPay's refund endpoint here
        String refundTxnId = "REFUND_" + System.currentTimeMillis(); // Placeholder transaction ID
        log.warn("REFUND API NOT IMPLEMENTED YET. Using placeholder transaction ID: {}", refundTxnId);

        // Update payment with refund information
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(refundAmount);
        payment.setRefundTransactionId(refundTxnId);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);
        paymentRepo.save(payment);

        log.info("Refund initiated for Booking ID: {}, Amount: {}, Reason: {}", bookingId, refundAmount, reason);

        // Publish RefundCompletedEvent
        com.soa.common.event.RefundCompletedEvent event = com.soa.common.event.RefundCompletedEvent.builder()
                .bookingId(bookingId)
                .userId(payment.getUserId())
                .refundAmount(refundAmount)
                .refundTransactionId(refundTxnId)
                .build();

        eventPublisher.saveEventToOutbox("Payment", payment.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_REFUND_COMPLETED, event);

        log.info("Published RefundCompletedEvent for Booking ID: {}", bookingId);
    }

    private void handlePaymentFailed(Long bookingId, String reason) {
        paymentRepo.findByBookingId(bookingId).ifPresent(payment -> handlePaymentFailed(payment, reason));
    }

    private void handlePaymentFailed(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepo.save(payment);

        publishPaymentEvent(payment, RabbitMQConfig.ROUTING_KEY_PAYMENT_FAILED, "FAILED");
    }

    private void publishPaymentEvent(Payment payment, String routingKey, String statusMsg) {
        Object event;
        if ("SUCCESS".equals(statusMsg)) {
            event = PaymentCompletedEvent.builder()
                    .bookingId(payment.getBookingId())
                    .paymentId(payment.getId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .transactionId(payment.getGatewayTransactionId())
                    .paymentMethod(payment.getMethod() != null ? payment.getMethod().name() : null)
                    .build();
        } else {
            event = PaymentFailedEvent.builder()
                    .bookingId(payment.getBookingId())
                    .userId(payment.getUserId())
                    .reason(statusMsg)
                    .build();
        }

        eventPublisher.saveEventToOutbox(
                "Payment",
                payment.getId().toString(),
                routingKey,
                event);
        log.info("Published {} event for Booking ID {}", routingKey, payment.getBookingId());
    }

    private boolean verifySignature(Map<String, String> queryParams, String vnp_SecureHash) {
        Map<String, String> fields = new HashMap<>(queryParams);

        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String signValue = vnpayConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());

        return signValue.equals(vnp_SecureHash);
    }

    @Transactional
    public void handleBookingCancellation(BookingCancelledEvent event) {
        paymentRepo.findByBookingId(event.getBookingId()).ifPresent(payment -> {

            if (payment.getStatus() == PaymentStatus.CONFIRMED) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepo.save(payment);
                log.info("Payment for Booking ID {} has been cancelled.", event.getBookingId());

            } else if (payment.getStatus() == PaymentStatus.COMPLETED) {
                log.info("Booking ID {} was cancelled after payment. Initiating refund process...",
                        event.getBookingId());
            }
        });
    }

    @Transactional
    public void cancelPaymentForBooking(Long bookingId) {
        paymentRepo.findByBookingId(bookingId).ifPresent(payment -> {

            if (payment.getStatus() == PaymentStatus.CONFIRMED) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepo.save(payment);
                log.info("Payment for Booking ID {} was cancelled due to booking failure.", bookingId);
            } else {
                log.warn(
                        "Received booking failure event for Booking ID {}, but payment status is already {}. No action taken.",
                        bookingId, payment.getStatus());
            }
        });
    }

    @Transactional
    public void createAndConfirmPayment(BookingReadyForPaymentEvent event) {
        if (paymentRepo.findByBookingId(event.getBookingId()).isPresent()) {
            log.warn("Payment already exists for booking {}", event.getBookingId());
            return;
        }

        Payment payment = Payment.builder()
                .bookingId(event.getBookingId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .status(PaymentStatus.CONFIRMED)
                .build();

        paymentRepo.save(payment);
        log.info("Created and Confirmed payment for booking {}", event.getBookingId());
    }
}