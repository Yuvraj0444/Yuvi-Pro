package com.skyways.payment.service;

import com.skyways.common.exception.payment.DuplicatePaymentException;
import com.skyways.common.exception.payment.PaymentFailedException;
import com.skyways.common.exception.payment.RefundFailedException;
import com.skyways.payment.dto.CreateOrderResponse;
import com.skyways.payment.dto.RefundResult;
import com.skyways.payment.entity.Payment;
import com.skyways.payment.entity.PaymentStatus;
import com.skyways.payment.entity.PaymentTransaction;
import com.skyways.payment.entity.Refund;
import com.skyways.payment.kafka.PaymentEventProducer;
import com.skyways.payment.repository.PaymentRepository;
import com.skyways.payment.repository.PaymentTransactionRepository;
import com.skyways.payment.repository.RefundRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LogManager.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final RefundRepository refundRepository;
    private final RazorpayService razorpayService;
    private final PaymentEventProducer eventProducer;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentTransactionRepository transactionRepository,
                          RefundRepository refundRepository,
                          RazorpayService razorpayService,
                          PaymentEventProducer eventProducer) {
        this.paymentRepository     = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.refundRepository      = refundRepository;
        this.razorpayService       = razorpayService;
        this.eventProducer         = eventProducer;
    }

    /**
     * Step 1 — Called by the Kafka consumer when the saga initiates payment.
     * Creates a Razorpay Order and persists it as PENDING.
     * The frontend polls GET /api/v1/payments/orders/{bookingId} to retrieve
     * the order_id and render Razorpay Checkout.
     */
    @Transactional
    public CreateOrderResponse createOrder(UUID bookingId, BigDecimal amount,
                                           String currency, String sagaId) {
        String idempotencyKey = "SW-" + bookingId;

        // Idempotent — return existing order if already created for this booking
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new DuplicatePaymentException(idempotencyKey));

            log.warn("Duplicate createOrder call [bookingId={}, orderId={}]",
                bookingId, existing.getRazorpayOrderId());

            return CreateOrderResponse.builder()
                .razorpayOrderId(existing.getRazorpayOrderId())
                .amountInSmallestUnit(amount.multiply(BigDecimal.valueOf(100)).longValue())
                .currency(currency.toUpperCase())
                .build();
        }

        CreateOrderResponse orderResponse = razorpayService.createOrder(
            amount, currency, bookingId.toString(), idempotencyKey);

        Payment payment = Payment.builder()
            .bookingId(bookingId)
            .amount(amount)
            .currency(currency.toUpperCase())
            .status(PaymentStatus.PENDING)
            .razorpayOrderId(orderResponse.getRazorpayOrderId())
            .idempotencyKey(idempotencyKey)
            .sagaId(sagaId)
            .build();

        paymentRepository.save(payment);

        log.info("Payment order created [bookingId={}, razorpayOrderId={}, sagaId={}]",
            bookingId, orderResponse.getRazorpayOrderId(), sagaId);

        return orderResponse;
    }

    /**
     * Step 2 — Called by the HTTP endpoint after Razorpay Checkout completes.
     * Verifies the HMAC-SHA256 signature, marks payment COMPLETED,
     * and publishes payment-processed to Kafka so the saga can proceed.
     */
    @Transactional
    public Payment verifyAndCapture(String razorpayOrderId, String razorpayPaymentId,
                                    String razorpaySignature) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() -> new PaymentFailedException(
                "No payment found for Razorpay order: " + razorpayOrderId));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("Payment already captured [razorpayOrderId={}]", razorpayOrderId);
            return payment;
        }

        try {
            razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

            payment.setGatewayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment = paymentRepository.save(payment);

            recordTransaction(payment, "payment.captured", razorpayPaymentId);

            log.info("Payment captured [paymentId={}, bookingId={}, razorpayPaymentId={}]",
                payment.getPaymentId(), payment.getBookingId(), razorpayPaymentId);

            eventProducer.publishPaymentProcessed(payment, payment.getSagaId());

            return payment;

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            recordTransaction(payment, "payment.failed", e.getMessage());

            log.error("Payment capture failed [razorpayOrderId={}]: {}", razorpayOrderId, e.getMessage());

            eventProducer.publishPaymentFailed(payment, payment.getSagaId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Issues a full refund via Razorpay. Called when the saga compensates a failed booking.
     */
    @Transactional
    public Refund processRefund(UUID bookingId, String reason) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new PaymentFailedException(
                "No payment found for booking: " + bookingId));

        if (payment.getGatewayPaymentId() == null) {
            throw new RefundFailedException(bookingId.toString(),
                "Payment was never captured — no Razorpay payment_id on record");
        }

        try {
            RefundResult result = razorpayService.refund(
                payment.getGatewayPaymentId(), payment.getAmount(), bookingId.toString());

            Refund refund = Refund.builder()
                .payment(payment)
                .refundAmount(payment.getAmount())
                .gatewayRefundId(result.getRefundId())
                .reason(reason)
                .status(PaymentStatus.REFUNDED)
                .build();

            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            log.info("Refund processed [refundId={}, bookingId={}, amount={}]",
                result.getRefundId(), bookingId, payment.getAmount());

            return refundRepository.save(refund);

        } catch (Exception e) {
            log.error("Refund failed [bookingId={}]: {}", bookingId, e.getMessage());
            throw e;
        }
    }

    public Payment getPaymentByBookingId(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new PaymentFailedException(
                "No payment found for booking: " + bookingId));
    }

    private void recordTransaction(Payment payment, String event, String rawResponse) {
        transactionRepository.save(PaymentTransaction.builder()
            .payment(payment)
            .gatewayEvent(event)
            .rawResponse(rawResponse)
            .build());
    }
}