package com.skyways.payment.kafka;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.payment.entity.Payment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentEventProducer {

    private static final Logger log = LogManager.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentProcessed(Payment payment, String sagaId) {
        KafkaEventEnvelope<Map<String, Object>> envelope =
            KafkaEventEnvelope.<Map<String, Object>>builder()
                .eventType("PAYMENT_PROCESSED")
                .serviceSource("skyways-payment-service")
                .sagaId(sagaId)
                .traceId(ThreadContext.get("traceId"))
                .payload(Map.of(
                    "paymentId",     payment.getPaymentId().toString(),
                    "bookingId",     payment.getBookingId().toString(),
                    "gatewayPaymentId", payment.getGatewayPaymentId(),
                    "amount",        payment.getAmount().toString(),
                    "currency",      payment.getCurrency()
                ))
                .build();

        kafkaTemplate.send(KafkaTopics.PAYMENT_PROCESSED,
                payment.getBookingId().toString(), envelope)
            .whenComplete((r, ex) -> {
                if (ex != null)
                    log.error("Failed to publish PAYMENT_PROCESSED [bookingId={}]: {}",
                        payment.getBookingId(), ex.getMessage());
                else
                    log.info("Published PAYMENT_PROCESSED [bookingId={}]", payment.getBookingId());
            });
    }

    public void publishPaymentFailed(Payment payment, String sagaId, String reason) {
        KafkaEventEnvelope<Map<String, Object>> envelope =
            KafkaEventEnvelope.<Map<String, Object>>builder()
                .eventType("PAYMENT_FAILED")
                .serviceSource("skyways-payment-service")
                .sagaId(sagaId)
                .traceId(ThreadContext.get("traceId"))
                .payload(Map.of(
                    "paymentId", payment.getPaymentId().toString(),
                    "bookingId", payment.getBookingId().toString(),
                    "reason",    reason
                ))
                .build();

        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED,
                payment.getBookingId().toString(), envelope)
            .whenComplete((r, ex) -> {
                if (ex != null)
                    log.error("Failed to publish PAYMENT_FAILED [bookingId={}]", payment.getBookingId());
                else
                    log.info("Published PAYMENT_FAILED [bookingId={}]", payment.getBookingId());
            });
    }
}
