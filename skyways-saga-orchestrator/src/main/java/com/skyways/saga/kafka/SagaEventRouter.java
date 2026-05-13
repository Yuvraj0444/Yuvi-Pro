package com.skyways.saga.kafka;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.saga.service.SagaOrchestrationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Single Kafka consumer class routing all saga-related events to the orchestration service.
 * Each listener group is unique to prevent cross-consumer interference.
 */
@Component
public class SagaEventRouter {

    private static final Logger log = LogManager.getLogger(SagaEventRouter.class);

    private final SagaOrchestrationService orchestrationService;

    public SagaEventRouter(SagaOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @KafkaListener(
        topics = KafkaTopics.BOOKING_INITIATED,
        groupId = "saga-orchestrator-booking-initiated"
    )
    public void onBookingInitiated(KafkaEventEnvelope<Map<String, Object>> event) {
        log.info("Routing BOOKING_INITIATED [sagaId={}, eventId={}]",
            event.getSagaId(), event.getEventId());
        try {
            orchestrationService.handleBookingInitiated(event.getPayload(), event.getSagaId());
        } catch (Exception e) {
            log.error("Error handling BOOKING_INITIATED [sagaId={}]: {}",
                event.getSagaId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = KafkaTopics.SEAT_RESERVATION_CONFIRMED,
        groupId = "saga-orchestrator-seat-confirmed"
    )
    public void onSeatReservationConfirmed(KafkaEventEnvelope<Map<String, Object>> event) {
        log.info("Routing SEAT_RESERVATION_CONFIRMED [sagaId={}]", event.getSagaId());
        try {
            orchestrationService.handleSeatReservationConfirmed(event.getPayload(), event.getSagaId());
        } catch (Exception e) {
            log.error("Error handling SEAT_RESERVATION_CONFIRMED [sagaId={}]: {}",
                event.getSagaId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = KafkaTopics.SEAT_RESERVATION_FAILED,
        groupId = "saga-orchestrator-seat-failed"
    )
    public void onSeatReservationFailed(KafkaEventEnvelope<Map<String, Object>> event) {
        log.warn("Routing SEAT_RESERVATION_FAILED [sagaId={}]", event.getSagaId());
        try {
            orchestrationService.handleSeatReservationFailed(event.getPayload(), event.getSagaId());
        } catch (Exception e) {
            log.error("Error handling SEAT_RESERVATION_FAILED [sagaId={}]: {}",
                event.getSagaId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = KafkaTopics.PAYMENT_PROCESSED,
        groupId = "saga-orchestrator-payment-processed"
    )
    public void onPaymentProcessed(KafkaEventEnvelope<Map<String, Object>> event) {
        log.info("Routing PAYMENT_PROCESSED [sagaId={}]", event.getSagaId());
        try {
            orchestrationService.handlePaymentProcessed(event.getPayload(), event.getSagaId());
        } catch (Exception e) {
            log.error("Error handling PAYMENT_PROCESSED [sagaId={}]: {}",
                event.getSagaId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = KafkaTopics.PAYMENT_FAILED,
        groupId = "saga-orchestrator-payment-failed"
    )
    public void onPaymentFailed(KafkaEventEnvelope<Map<String, Object>> event) {
        log.warn("Routing PAYMENT_FAILED [sagaId={}]", event.getSagaId());
        try {
            orchestrationService.handlePaymentFailed(event.getPayload(), event.getSagaId());
        } catch (Exception e) {
            log.error("Error handling PAYMENT_FAILED [sagaId={}]: {}",
                event.getSagaId(), e.getMessage(), e);
        }
    }
}
