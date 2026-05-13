package com.skyways.saga.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_state", indexes = {
    @Index(name = "idx_saga_booking", columnList = "booking_id"),
    @Index(name = "idx_saga_status",  columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {

    @Id
    @Column(name = "saga_id", updatable = false, nullable = false)
    private UUID sagaId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = "current_step", length = 50)
    private String currentStep;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "compensation_step", length = 50)
    private String compensationStep;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
