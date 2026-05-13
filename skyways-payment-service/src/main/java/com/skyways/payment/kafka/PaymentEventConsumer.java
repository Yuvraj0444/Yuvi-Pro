package com.skyways.payment.kafka;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.payment.service.PaymentService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LogManager.getLogger(PaymentEventConsumer.class);

    private final PaymentService paymentService;

    public PaymentEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Receives saga event → creates a Razorpay Order (step 1).
     * The frontend then calls GET /api/v1/payments/orders/{bookingId}
     * to retrieve the order_id and render Razorpay Checkout.
     */
    @KafkaListener(
        topics = KafkaTopics.PAYMENT_INITIATION_REQUESTED,
        groupId = "payment-service-group"
    )
    public void handlePaymentInitiationRequested(KafkaEventEnvelope<Map<String, Object>> event) {
        Map<String, Object> payload = event.getPayload();
        String bookingId = (String) payload.get("bookingId");
        String amountStr = (String) payload.get("amount");
        String currency  = (String) payload.getOrDefault("currency", "INR");
        String sagaId    = event.getSagaId();

        log.info("Payment initiation requested [sagaId={}, bookingId={}, amount={} {}]",
            sagaId, bookingId, amountStr, currency);

        try {
            paymentService.createOrder(
                UUID.fromString(bookingId),
                new BigDecimal(amountStr),
                currency,
                sagaId
            );
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for booking {}: {}", bookingId, e.getMessage());
        }
    }

    /**
     * Triggers a Razorpay refund when the saga compensates a cancelled booking.
     */
    @KafkaListener(
        topics = KafkaTopics.BOOKING_CANCELLED,
        groupId = "payment-service-refund-group"
    )
    public void handleBookingCancelledForRefund(KafkaEventEnvelope<Map<String, Object>> event) {
        String bookingId = (String) event.getPayload().get("bookingId");
        String reason    = (String) event.getPayload().getOrDefault("reason", "Booking cancelled");

        log.info("Processing refund for cancelled booking [bookingId={}]", bookingId);

        try {
            paymentService.processRefund(UUID.fromString(bookingId), reason);
        } catch (Exception e) {
            log.error("Refund failed for cancelled booking {}: {}", bookingId, e.getMessage());
        }
    }
}