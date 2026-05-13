package com.skyways.booking.kafka;

import com.skyways.booking.entity.BookingStatus;
import com.skyways.booking.service.BookingService;
import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.kafka.KafkaEventEnvelope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class SagaEventConsumer {

    private static final Logger log = LogManager.getLogger(SagaEventConsumer.class);

    private final BookingService bookingService;
    private final BookingEventProducer bookingEventProducer;

    public SagaEventConsumer(BookingService bookingService,
                              BookingEventProducer bookingEventProducer) {
        this.bookingService = bookingService;
        this.bookingEventProducer = bookingEventProducer;
    }

    @KafkaListener(
        topics = KafkaTopics.BOOKING_CONFIRMED,
        groupId = "booking-service-group"
    )
    public void handleBookingConfirmed(KafkaEventEnvelope<Map<String, Object>> event) {
        String bookingId = (String) event.getPayload().get("bookingId");
        log.info("Updating booking to CONFIRMED [bookingId={}, sagaId={}]",
            bookingId, event.getSagaId());

        try {
            bookingService.updateBookingStatus(
                UUID.fromString(bookingId), BookingStatus.CONFIRMED, "Payment processed");
        } catch (Exception e) {
            log.error("Failed to confirm booking [bookingId={}]: {}", bookingId, e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = KafkaTopics.BOOKING_CANCELLED,
        groupId = "booking-service-group"
    )
    public void handleBookingCancelled(KafkaEventEnvelope<Map<String, Object>> event) {
        String bookingId = (String) event.getPayload().get("bookingId");
        String reason = (String) event.getPayload().getOrDefault("reason", "Saga compensation");
        log.info("Updating booking to CANCELLED [bookingId={}, sagaId={}]",
            bookingId, event.getSagaId());

        try {
            bookingService.updateBookingStatus(
                UUID.fromString(bookingId), BookingStatus.CANCELLED, reason);
        } catch (Exception e) {
            log.error("Failed to cancel booking [bookingId={}]: {}", bookingId, e.getMessage(), e);
        }
    }
}
