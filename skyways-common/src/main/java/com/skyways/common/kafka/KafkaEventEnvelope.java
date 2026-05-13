package com.skyways.common.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaEventEnvelope<T> {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String serviceSource;
    private String sagaId;
    private String traceId;
    private T payload;
    @Builder.Default
    private Instant occurredAt = Instant.now();
    private int retryCount;
}
