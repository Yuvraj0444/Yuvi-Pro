package com.skyways.flight.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seats_flight_reserved", columnList = "flight_id, is_reserved")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_id", updatable = false, nullable = false)
    private UUID seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "seat_number", nullable = false, length = 5)
    private String seatNumber;

    @Column(name = "class_type", length = 20)
    private String classType;

    @Column(name = "is_reserved", nullable = false)
    @Builder.Default
    private boolean isReserved = false;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    /** Cross-service logical FK only — not a DB constraint */
    @Column(name = "booking_id")
    private UUID bookingId;
}
