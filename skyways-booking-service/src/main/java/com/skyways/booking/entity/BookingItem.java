package com.skyways.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id", updatable = false, nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "seat_id")
    private UUID seatId;

    @Column(name = "fare_class", length = 20)
    private String fareClass;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "segment_seq")
    @Builder.Default
    private int segmentSeq = 1;
}
