package com.skyways.booking.kafka;

import com.skyways.booking.entity.Booking;
import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.kafka.KafkaEventEnvelope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class BookingEventProducer {

    private static final Logger log = LogManager.getLogger(BookingEventProducer.class);

    private final KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate;

    public BookingEventProducer(KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookingInitiated(Booking booking, UUID flightId, int seatCount) {
        KafkaEventEnvelope<Map<String, Object>> envelope =
            KafkaEventEnvelope.<Map<String, Object>>builder()
                .eventType("BOOKING_INITIATED")
                .serviceSource("skyways-booking-service")
                .sagaId(booking.getSagaId().toString())
                .traceId(ThreadContext.get("traceId"))
                .payload(Map.of(
                    "bookingId",  booking.getBookingId().toString(),
                    "bookingRef", booking.getBookingRef(),
                    "userId",     booking.getUserId().toString(),
                    "flightId",   flightId.toString(),
                    "seatCount",  seatCount,
                    "amount",     booking.getTotalAmount().toString(),
                    "currency",   booking.getCurrency()
                ))
                .build();

        kafkaTemplate.send(KafkaTopics.BOOKING_INITIATED,
                booking.getBookingId().toString(), envelope)
            .whenComplete((r, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish BOOKING_INITIATED [bookingId={}]: {}",
                        booking.getBookingId(), ex.getMessage());
                } else {
                    log.info("Published BOOKING_INITIATED [bookingId={}, offset={}]",
                        booking.getBookingId(), r.getRecordMetadata().offset());
                }
            });
    }

    public void publishBookingConfirmed(String bookingId, String sagaId) {
        publish("BOOKING_CONFIRMED", KafkaTopics.BOOKING_CONFIRMED, bookingId, sagaId,
            Map.of("bookingId", bookingId));
    }

    public void publishBookingCancelled(String bookingId, String sagaId, String reason) {
        publish("BOOKING_CANCELLED", KafkaTopics.BOOKING_CANCELLED, bookingId, sagaId,
            Map.of("bookingId", bookingId, "reason", reason));
    }

    private void publish(String eventType, String topic, String key,
                          String sagaId, Map<String, Object> payload) {
        KafkaEventEnvelope<Map<String, Object>> envelope =
            KafkaEventEnvelope.<Map<String, Object>>builder()
                .eventType(eventType)
                .serviceSource("skyways-booking-service")
                .sagaId(sagaId)
                .traceId(ThreadContext.get("traceId"))
                .payload(payload)
                .build();

        kafkaTemplate.send(topic, key, envelope)
            .whenComplete((r, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} [key={}]: {}", eventType, key, ex.getMessage());
                } else {
                    log.info("Published {} [key={}]", eventType, key);
                }
            });
    }
}
