package com.skyways.notification.retry;

import com.skyways.common.enums.KafkaTopics;
import com.skyways.common.exception.notification.NotificationDeliveryException;
import com.skyways.common.kafka.KafkaEventEnvelope;
import com.skyways.notification.dto.BookingConfirmationDto;
import com.skyways.notification.service.SendGridService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationRetryHandler {

    private static final Logger log = LogManager.getLogger(NotificationRetryHandler.class);

    private final SendGridService sendGridService;
    private final KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate;

    public NotificationRetryHandler(SendGridService sendGridService,
                                     KafkaTemplate<String, KafkaEventEnvelope<?>> kafkaTemplate) {
        this.sendGridService = sendGridService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Retryable(
        retryFor = NotificationDeliveryException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public void sendWithRetry(BookingConfirmationDto dto) {
        if ("CONFIRMED".equals(dto.getEventType())) {
            sendGridService.sendBookingConfirmation(dto);
        } else {
            sendGridService.sendCancellationNotification(dto);
        }
    }

    @Recover
    public void sendToDLQ(NotificationDeliveryException ex, BookingConfirmationDto dto) {
        log.error("All retries exhausted for notification [bookingRef={}, email={}]. " +
                  "Routing to DLQ. Error: {}",
            dto.getBookingRef(), dto.getPassengerEmail(), ex.getMessage());

        KafkaEventEnvelope<Map<String, String>> dlqEvent =
            KafkaEventEnvelope.<Map<String, String>>builder()
                .eventType("NOTIFICATION_DELIVERY_FAILED")
                .serviceSource("skyways-notification-service")
                .payload(Map.of(
                    "bookingRef",     dto.getBookingRef(),
                    "passengerEmail", dto.getPassengerEmail(),
                    "eventType",      dto.getEventType(),
                    "error",          ex.getMessage()
                ))
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_DLQ, dto.getBookingRef(), dlqEvent);
    }
}
