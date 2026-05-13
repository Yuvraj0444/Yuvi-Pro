package com.skyways.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_booking",     columnList = "booking_id"),
    @Index(name = "idx_payments_idempkey",    columnList = "idempotency_key"),
    @Index(name = "idx_payments_rzp_order",   columnList = "razorpay_order_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    @Column(name = "booking_id", unique = true, nullable = false)
    private UUID bookingId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Razorpay order_id — created in step 1 of the two-step payment flow */
    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;

    /** Razorpay payment_id — received after customer completes checkout */
    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    /** Prevents duplicate order creation for the same booking */
    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "saga_id")
    private String sagaId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}