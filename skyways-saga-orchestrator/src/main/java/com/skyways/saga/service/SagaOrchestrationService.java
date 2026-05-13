package com.skyways.saga.service;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.exception.booking.SagaCompensationException;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.saga.entity.SagaState;
import com.skyways.saga.entity.SagaStatus;
import com.skyways.saga.repository.SagaStateRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the booking saga across:
 * booking-initiated → seat-reservation-requested
 *   ↓ seat-reservation-confirmed → payment-initiation-requested
 *     ↓ payment-processed → booking-confirmed + notification-requested
 *
 * Compensation (on any failure):
 * payment-failed / seat-reservation-failed
 *   → seat-release-requested → booking-cancelled → notification-requested (cancellation)
 */
@Service
public class SagaOrchestrationService {

    private static final Logger log = LogManager.getLogger(SagaOrchestrationService.class);

    private final SagaStateRepository sagaStateRepository;
    private final KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate;

    public SagaOrchestrationService(SagaStateRepository sagaStateRepository,
                                     KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate) {
        this.sagaStateRepository = sagaStateRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void handleBookingInitiated(Map<String, Object> payload, String sagaId) {
        UUID bookingId = UUID.fromString((String) payload.get("bookingId"));
        UUID sagaUUID  = UUID.fromString(sagaId);

        if (sagaStateRepository.existsById(sagaUUID)) {
            log.warn("Duplicate saga event for sagaId={}, bookingId={} — skipping",
                sagaId, bookingId);
            return;
        }

        SagaState state = SagaState.builder()
            .sagaId(sagaUUID)
            .bookingId(bookingId)
            .status(SagaStatus.SEAT_RESERVATION_PENDING)
            .currentStep("SEAT_RESERVATION")
            .build();
        sagaStateRepository.save(state);

        log.info("Saga started [sagaId={}, bookingId={}]", sagaId, bookingId);

        publish(KafkaTopics.SEAT_RESERVATION_REQUESTED, sagaId,
            (String) payload.get("bookingId"), "SEAT_RESERVATION_REQUESTED",
            Map.of(
                "bookingId", payload.get("bookingId"),
                "flightId",  payload.get("flightId"),
                "seatCount", payload.get("seatCount")
            ));
    }

    @Transactional
    public void handleSeatReservationConfirmed(Map<String, Object> payload, String sagaId) {
        String bookingId = (String) payload.get("bookingId");
        updateSagaState(sagaId, SagaStatus.PAYMENT_PENDING, "PAYMENT");

        log.info("Seat confirmed — initiating payment [sagaId={}, bookingId={}]",
            sagaId, bookingId);

        publish(KafkaTopics.PAYMENT_INITIATION_REQUESTED, sagaId, bookingId,
            "PAYMENT_INITIATION_REQUESTED",
            Map.of(
                "bookingId",    bookingId,
                "amount",       payload.getOrDefault("amount", "0"),
                "currency",     payload.getOrDefault("currency", "USD"),
                "paymentToken", payload.getOrDefault("paymentToken", "tok_visa")
            ));
    }

    @Transactional
    public void handleSeatReservationFailed(Map<String, Object> payload, String sagaId) {
        String bookingId = (String) payload.get("bookingId");
        String reason    = (String) payload.getOrDefault("reason", "Seat reservation failed");

        log.warn("Seat reservation failed — starting compensation [sagaId={}, bookingId={}]",
            sagaId, bookingId);

        updateSagaState(sagaId, SagaStatus.COMPENSATING, "BOOKING_CANCEL");

        publishBookingCancelled(sagaId, bookingId, reason);
        publishNotification(sagaId, bookingId, payload, "CANCELLED", reason);
        updateSagaState(sagaId, SagaStatus.COMPENSATED, null);
    }

    @Transactional
    public void handlePaymentProcessed(Map<String, Object> payload, String sagaId) {
        String bookingId = (String) payload.get("bookingId");
        updateSagaState(sagaId, SagaStatus.COMPLETED, null);

        log.info("Payment processed — completing saga [sagaId={}, bookingId={}]",
            sagaId, bookingId);

        publish(KafkaTopics.BOOKING_CONFIRMED, sagaId, bookingId, "BOOKING_CONFIRMED",
            Map.of("bookingId", bookingId));

        publishNotification(sagaId, bookingId, payload, "CONFIRMED", null);
    }

    @Transactional
    public void handlePaymentFailed(Map<String, Object> payload, String sagaId) {
        String bookingId = (String) payload.get("bookingId");
        String reason    = (String) payload.getOrDefault("reason", "Payment declined");

        log.warn("Payment failed — compensating [sagaId={}, bookingId={}, reason={}]",
            sagaId, bookingId, reason);

        updateSagaState(sagaId, SagaStatus.COMPENSATING, "SEAT_RELEASE");

        try {
            publish(KafkaTopics.SEAT_RELEASE_REQUESTED, sagaId, bookingId,
                "SEAT_RELEASE_REQUESTED", Map.of("bookingId", bookingId));
        } catch (Exception e) {
            log.error("Failed to publish seat release for saga {} — manual intervention required",
                sagaId, e);
            throw new SagaCompensationException(sagaId, "SEAT_RELEASE", e);
        }

        publishBookingCancelled(sagaId, bookingId, reason);
        publishNotification(sagaId, bookingId, payload, "CANCELLED", reason);
        updateSagaState(sagaId, SagaStatus.COMPENSATED, null);
    }

    private void updateSagaState(String sagaId, SagaStatus newStatus, String step) {
        sagaStateRepository.findById(UUID.fromString(sagaId)).ifPresent(state -> {
            state.setStatus(newStatus);
            if (step != null) state.setCurrentStep(step);
            sagaStateRepository.save(state);
        });
    }

    private void publishBookingCancelled(String sagaId, String bookingId, String reason) {
        publish(KafkaTopics.BOOKING_CANCELLED, sagaId, bookingId, "BOOKING_CANCELLED",
            Map.of("bookingId", bookingId, "reason", reason));
    }

    private void publishNotification(String sagaId, String bookingId,
                                      Map<String, Object> context, String eventType, String reason) {
        Map<String, Object> notifPayload = new java.util.HashMap<>(Map.of(
            "bookingRef",     context.getOrDefault("bookingRef", bookingId),
            "passengerEmail", context.getOrDefault("passengerEmail", ""),
            "passengerName",  context.getOrDefault("passengerName", "Valued Customer"),
            "flightNumber",   context.getOrDefault("flightNumber", ""),
            "originCity",     context.getOrDefault("originCity", ""),
            "destinationCity", context.getOrDefault("destinationCity", ""),
            "totalAmount",    context.getOrDefault("amount", "0"),
            "currency",       context.getOrDefault("currency", "USD"),
            "eventType",      eventType
        ));
        if (reason != null) notifPayload.put("cancellationReason", reason);

        publish(KafkaTopics.NOTIFICATION_REQUESTED, sagaId, bookingId,
            "NOTIFICATION_REQUESTED", notifPayload);
    }

    private void publish(String topic, String sagaId, String key,
                          String eventType, Map<String, Object> payload) {
        KafkaEventEnvelope<Map<String, Object>> envelope =
            KafkaEventEnvelope.<Map<String, Object>>builder()
                .eventType(eventType)
                .serviceSource("skyways-saga-orchestrator")
                .sagaId(sagaId)
                .payload(payload)
                .build();

        kafkaTemplate.send(topic, key, envelope)
            .whenComplete((r, ex) -> {
                if (ex != null)
                    log.error("Failed to publish {} [sagaId={}]: {}", eventType, sagaId, ex.getMessage());
                else
                    log.debug("Published {} [sagaId={}]", eventType, sagaId);
            });
    }
}
