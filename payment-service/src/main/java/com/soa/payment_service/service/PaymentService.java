package com.soa.payment_service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soa.payment_service.config.RabbitMQConfig;
import com.soa.payment_service.config.VNPayConfig;
import com.soa.payment_service.entity.Payment;
import com.soa.payment_service.entity.PaymentMethod;
import com.soa.payment_service.entity.PaymentStatus;
import com.soa.payment_service.repository.PaymentRepository;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final RabbitTemplate rabbitTemplate;
    private final VNPayConfig vnpayConfig; 

    @Transactional
    public void initPayment(Long bookingId, String userId, BigDecimal amount) {
        if (paymentRepo.findByBookingId(bookingId).isPresent()) return;
        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(userId)
                .amount(amount)
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepo.save(payment);
    }

    public String createVnPayUrl(Long bookingId, HttpServletRequest request) {
        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Booking already paid");
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
            throw new RuntimeException("Invalid Checksum");
        }

        Payment payment = paymentRepo.findByBookingId(Long.valueOf(bookingId))
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            return payment;
        }

        if ("00".equals(vnp_ResponseCode)) {
            log.info("Payment SUCCESS for booking {}", bookingId);
            
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setGatewayTransactionId(queryParams.get("vnp_TransactionNo"));
            paymentRepo.save(payment);

            publishPaymentEvent(payment, RabbitMQConfig.ROUTING_KEY_PAYMENT_COMPLETED, "SUCCESS");
            
            return payment;
        } else {
            log.warn("Payment FAILED for booking {} with code {}", bookingId, vnp_ResponseCode);
            
            handlePaymentFailed(payment, "VNPay Response Code: " + vnp_ResponseCode);
            
            throw new RuntimeException("Payment Failed at Gateway");
        }
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
        Map<String, Object> event = new HashMap<>();
        event.put("bookingId", payment.getBookingId());
        event.put("paymentId", payment.getId());
        event.put("userId", payment.getUserId());
        event.put("amount", payment.getAmount());
        event.put("transactionId", payment.getGatewayTransactionId());
        event.put("status", statusMsg); 

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
        log.info("Published {} for booking {}", routingKey, payment.getBookingId());
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
}