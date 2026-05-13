package com.skyways.payment.controller;

import com.skyways.common.dto.ApiResponse;
import com.skyways.payment.dto.CreateOrderResponse;
import com.skyways.payment.dto.VerifyPaymentRequest;
import com.skyways.payment.entity.Payment;
import com.skyways.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Razorpay order creation, signature verification, and payment status")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private static final Logger log = LogManager.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Get Razorpay order for a booking",
        description = "Returns razorpayOrderId, amount (in smallest currency unit), and currency. Call this after booking creation to initialize Razorpay Checkout on the frontend.")
    @GetMapping("/orders/{bookingId}")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> getOrderDetails(
            @Parameter(description = "UUID of the booking") @PathVariable UUID bookingId) {

        Payment payment = paymentService.getPaymentByBookingId(bookingId);

        CreateOrderResponse order = CreateOrderResponse.builder()
            .razorpayOrderId(payment.getRazorpayOrderId())
            .amountInSmallestUnit(payment.getAmount()
                .multiply(java.math.BigDecimal.valueOf(100)).longValue())
            .currency(payment.getCurrency())
            .build();

        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @Operation(summary = "Verify Razorpay payment signature",
        description = "Validates HMAC-SHA256 signature from the Razorpay Checkout callback. On success, marks payment COMPLETED and publishes payment-processed to Kafka, triggering booking confirmation.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Payment>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest req) {

        log.info("Verifying Razorpay payment [orderId={}]", req.getRazorpayOrderId());

        Payment payment = paymentService.verifyAndCapture(
            req.getRazorpayOrderId(),
            req.getRazorpayPaymentId(),
            req.getRazorpaySignature()
        );

        return ResponseEntity.ok(ApiResponse.ok(payment));
    }

    @Operation(summary = "Get payment status for a booking",
        description = "Returns the current payment state (PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED)")
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentStatus(
            @Parameter(description = "UUID of the booking") @PathVariable UUID bookingId) {

        Payment payment = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(payment));
    }
}