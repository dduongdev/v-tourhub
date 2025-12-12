package com.soa.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.soa.payment_service.entity.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);
}
