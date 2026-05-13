package com.skyways.flight.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fare_classes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fare_id", updatable = false, nullable = false)
    private UUID fareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "class_type", nullable = false, length = 20)
    private String classType;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false)
    private int available;
}
