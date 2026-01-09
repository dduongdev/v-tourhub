package com.soa.payment_service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String VNPAY_REFUND_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private static final DateTimeFormatter VN_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Initiate refund for a completed payment using VNPay Refund API
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

        // Validation: Must have gateway transaction ID for refund
        if (payment.getGatewayTransactionId() == null || payment.getGatewayTransactionId().isEmpty()) {
            throw new BusinessException("Cannot refund: Original transaction ID is missing");
        }

        // Call VNPay Refund API
        String refundTxnId = callVnPayRefundApi(payment, refundAmount, reason);

        // Update payment with refund information
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(refundAmount);
        payment.setRefundTransactionId(refundTxnId);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);
        paymentRepo.save(payment);

        log.info("Refund completed for Booking ID: {}, Amount: {}, RefundTxnId: {}", bookingId, refundAmount,
                refundTxnId);

        // Publish RefundCompletedEvent
        com.soa.common.event.RefundCompletedEvent event = com.soa.common.event.RefundCompletedEvent.builder()
                .bookingId(bookingId)
                .userId(payment.getUserId())
                .refundAmount(refundAmount)
                .refundTransactionId(refundTxnId)
                .customerEmail(payment.getCustomerEmail())
                .build();

        eventPublisher.saveEventToOutbox("Payment", payment.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_REFUND_COMPLETED, event);

        log.info("Published RefundCompletedEvent for Booking ID: {}", bookingId);
    }

    /**
     * Convenience method for initiating refund (called from
     * handleBookingCancellation)
     */
    @Transactional
    public void initRefund(Long bookingId, BigDecimal refundAmount, String reason) {
        initiateRefund(bookingId, reason, refundAmount);
    }

    /**
     * Call VNPay Refund API to process the refund
     * 
     * @param payment      - the original payment record
     * @param refundAmount - amount to refund
     * @param reason       - reason for refund
     * @return refund transaction ID from VNPay
     */
    private String callVnPayRefundApi(Payment payment, BigDecimal refundAmount, String reason) {
        try {
            ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
            String vnp_RequestId = vnpayConfig.getRandomNumber(8);
            String vnp_CreateDate = now.format(VN_DATETIME_FORMATTER);

            // Determine transaction type: 02 = Full Refund, 03 = Partial Refund
            String vnp_TransactionType = refundAmount.compareTo(payment.getAmount()) == 0 ? "02" : "03";

            // Build refund request parameters
            Map<String, String> vnp_Params = new LinkedHashMap<>();
            vnp_Params.put("vnp_RequestId", vnp_RequestId);
            vnp_Params.put("vnp_Version", vnpayConfig.getVnp_Version());
            vnp_Params.put("vnp_Command", "refund");
            vnp_Params.put("vnp_TmnCode", vnpayConfig.getVnp_TmnCode());
            vnp_Params.put("vnp_TransactionType", vnp_TransactionType);
            vnp_Params.put("vnp_TxnRef", String.valueOf(payment.getBookingId()));
            vnp_Params.put("vnp_Amount", String.valueOf(refundAmount.longValue() * 100));
            vnp_Params.put("vnp_OrderInfo", reason != null ? reason : "Refund for booking " + payment.getBookingId());
            vnp_Params.put("vnp_TransactionNo", payment.getGatewayTransactionId());
            vnp_Params.put("vnp_TransactionDate", formatPaidAtDate(payment.getPaidAt()));
            vnp_Params.put("vnp_CreateBy", payment.getUserId() != null ? payment.getUserId() : "system");
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1"); // Server IP

            // Generate secure hash
            String signData = buildSignData(vnp_Params);
            String vnp_SecureHash = vnpayConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), signData);
            vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

            log.info("Calling VNPay Refund API for Booking ID: {}, Amount: {}, TransactionType: {}",
                    payment.getBookingId(), refundAmount, vnp_TransactionType);

            // Call VNPay Refund API
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(vnp_Params);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    VNPAY_REFUND_URL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            log.debug("VNPay Refund API Response: {}", response.getBody());

            // Parse response
            JsonNode responseJson = mapper.readTree(response.getBody());
            String responseCode = responseJson.has("vnp_ResponseCode") ? responseJson.get("vnp_ResponseCode").asText()
                    : null;
            String refundTxnId = responseJson.has("vnp_TransactionNo") ? responseJson.get("vnp_TransactionNo").asText()
                    : vnp_RequestId;

            // VNPay response codes: 00 = success
            if ("00".equals(responseCode)) {
                log.info("VNPay Refund successful. Transaction ID: {}", refundTxnId);
                return refundTxnId;
            } else {
                // Handle sandbox limitation or actual failure
                String message = responseJson.has("vnp_Message") ? responseJson.get("vnp_Message").asText()
                        : "Unknown error";

                // In sandbox, refund may not be fully supported
                // Log warning but proceed with local refund tracking
                log.warn("VNPay Refund API returned code: {}, message: {}. " +
                        "Processing local refund record.", responseCode, message);
                return "LOCAL_REFUND_" + vnp_RequestId;
            }

        } catch (Exception e) {
            log.error("Error calling VNPay Refund API for Booking ID: {}", payment.getBookingId(), e);
            // In case of API failure, still process local refund for tracking
            // Production should handle this differently based on business requirements
            log.warn("Proceeding with local refund tracking due to API error");
            return "LOCAL_REFUND_" + System.currentTimeMillis();
        }
    }

    /**
     * Build sign data string from parameters for HMAC signature
     */
    private String buildSignData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        // Order matters for VNPay signature
        String[] signFields = { "vnp_RequestId", "vnp_Version", "vnp_Command", "vnp_TmnCode",
                "vnp_TransactionType", "vnp_TxnRef", "vnp_Amount", "vnp_TransactionNo",
                "vnp_TransactionDate", "vnp_CreateBy", "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo" };

        for (int i = 0; i < signFields.length; i++) {
            String field = signFields[i];
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                sb.append(value);
                if (i < signFields.length - 1) {
                    sb.append("|");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Format paidAt date to VNPay format
     */
    private String formatPaidAtDate(LocalDateTime paidAt) {
        if (paidAt == null) {
            return ZonedDateTime.now(VIETNAM_ZONE).format(VN_DATETIME_FORMATTER);
        }
        return paidAt.atZone(VIETNAM_ZONE).format(VN_DATETIME_FORMATTER);
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
                initiateRefund(payment.getBookingId(), "Booking cancelled", payment.getAmount());
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
                .customerEmail(event.getCustomerEmail()) // Store for refund notification
                .build();

        paymentRepo.save(payment);
        log.info("Created and Confirmed payment for booking {}", event.getBookingId());
    }
}