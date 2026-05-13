package com.skyways.flight.kafka;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.exception.flight.FlightNotFoundException;
import com.skyways.common.exception.flight.FlightOverBookedException;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.flight.entity.Seat;
import com.skyways.flight.service.SeatService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SeatReservationConsumer {

    private static final Logger log = LogManager.getLogger(SeatReservationConsumer.class);

    private final SeatService seatService;
    private final KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate;

    public SeatReservationConsumer(SeatService seatService,
                                    KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate) {
        this.seatService = seatService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
        topics = KafkaTopics.SEAT_RESERVATION_REQUESTED,
        groupId = "flight-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSeatReservationRequested(KafkaEventEnvelope<Map<String, Object>> event) {
        String sagaId    = event.getSagaId();
        String traceId   = event.getTraceId();
        String flightId  = (String) event.getPayload().get("flightId");
        String bookingId = (String) event.getPayload().get("bookingId");
        int seatCount    = ((Number) event.getPayload().get("seatCount")).intValue();

        log.info("Processing seat reservation [sagaId={}, flightId={}, bookingId={}, seats={}]",
            sagaId, flightId, bookingId, seatCount);

        try {
            List<Seat> reserved = seatService.reserveSeats(
                UUID.fromString(flightId), UUID.fromString(bookingId), seatCount);

            KafkaEventEnvelope<Map<String, Object>> confirmed =
                KafkaEventEnvelope.<Map<String, Object>>builder()
                    .eventType("SEAT_RESERVATION_CONFIRMED")
                    .serviceSource("skyways-flight-service")
                    .sagaId(sagaId)
                    .traceId(traceId)
                    .payload(Map.of(
                        "bookingId", bookingId,
                        "flightId", flightId,
                        "reservedSeatIds", reserved.stream()
                            .map(s -> s.getSeatId().toString()).toList()
                    ))
                    .build();

            kafkaTemplate.send(KafkaTopics.SEAT_RESERVATION_CONFIRMED, bookingId, confirmed);

        } catch (FlightNotFoundException | FlightOverBookedException e) {
            log.warn("Seat reservation failed [sagaId={}, bookingId={}]: {}",
                sagaId, bookingId, e.getMessage());
            publishReservationFailed(sagaId, traceId, bookingId, flightId, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected seat reservation error [sagaId={}, bookingId={}]",
                sagaId, bookingId, e);
            publishReservationFailed(sagaId, traceId, bookingId, flightId, "Internal error");
        }
    }

    @KafkaListener(
        topics = KafkaTopics.SEAT_RELEASE_REQUESTED,
        groupId = "flight-service-group"
    )
    public void handleSeatReleaseRequested(KafkaEventEnvelope<Map<String, Object>> event) {
        String bookingId = (String) event.getPayload().get("bookingId");
        log.info("Processing seat release [sagaId={}, bookingId={}]", event.getSagaId(), bookingId);

        try {
            seatService.releaseSeats(UUID.fromString(bookingId));
            log.info("Seats released for booking {}", bookingId);
        } catch (Exception e) {
            log.error("Seat release failed for booking {}", bookingId, e);
        }
    }

    private void publishReservationFailed(String sagaId, String traceId,
                                           String bookingId, String flightId, String reason) {
        KafkaEventEnvelope<Map<String, Object>> failed =
            KafkaEventEnvelope.<Map<String, Object>>builder()
                .eventType("SEAT_RESERVATION_FAILED")
                .serviceSource("skyways-flight-service")
                .sagaId(sagaId)
                .traceId(traceId)
                .payload(Map.of(
                    "bookingId", bookingId,
                    "flightId", flightId,
                    "reason", reason
                ))
                .build();

        kafkaTemplate.send(KafkaTopics.SEAT_RESERVATION_FAILED, bookingId, failed);
    }
}
