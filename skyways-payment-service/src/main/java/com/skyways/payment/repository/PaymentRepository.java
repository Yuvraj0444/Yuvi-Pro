package com.skyways.payment.repository;

import com.skyways.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    boolean existsByIdempotencyKey(String idempotencyKey);
}