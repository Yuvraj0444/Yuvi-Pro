package com.skyways.notification.kafka;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.notification.dto.BookingConfirmationDto;
import com.skyways.notification.retry.NotificationRetryHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LogManager.getLogger(NotificationEventConsumer.class);

    private final NotificationRetryHandler retryHandler;

    public NotificationEventConsumer(NotificationRetryHandler retryHandler) {
        this.retryHandler = retryHandler;
    }

    @KafkaListener(
        topics = KafkaTopics.NOTIFICATION_REQUESTED,
        groupId = "notification-service-group"
    )
    public void handleNotificationRequested(KafkaEventEnvelope<Map<String, Object>> event) {
        Map<String, Object> payload = event.getPayload();
        String bookingRef    = (String) payload.get("bookingRef");
        String passengerEmail = (String) payload.get("passengerEmail");
        String eventType     = (String) payload.getOrDefault("eventType", "CONFIRMED");

        log.info("Notification requested [bookingRef={}, eventType={}, traceId={}]",
            bookingRef, eventType, event.getTraceId());

        try {
            BookingConfirmationDto dto = BookingConfirmationDto.builder()
                .bookingRef(bookingRef)
                .passengerEmail(passengerEmail)
                .passengerName((String) payload.getOrDefault("passengerName", "Valued Customer"))
                .flightNumber((String) payload.getOrDefault("flightNumber", ""))
                .originCity((String) payload.getOrDefault("originCity", ""))
                .destinationCity((String) payload.getOrDefault("destinationCity", ""))
                .departureTime((String) payload.getOrDefault("departureTime", ""))
                .arrivalTime((String) payload.getOrDefault("arrivalTime", ""))
                .totalAmount(payload.get("totalAmount") != null
                    ? new BigDecimal(payload.get("totalAmount").toString()) : BigDecimal.ZERO)
                .currency((String) payload.getOrDefault("currency", "USD"))
                .eventType(eventType)
                .build();

            retryHandler.sendWithRetry(dto);

            log.info("Notification sent [bookingRef={}, eventType={}]", bookingRef, eventType);

        } catch (Exception e) {
            log.error("Notification processing failed [bookingRef={}, eventType={}]: {}",
                bookingRef, eventType, e.getMessage(), e);
        }
    }
}
