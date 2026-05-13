package com.skyways.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @Column
    private String reason;

    @Column(name = "changed_at")
    @Builder.Default
    private Instant changedAt = Instant.now();
}
