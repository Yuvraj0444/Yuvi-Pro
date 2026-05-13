package com.skyways.flight.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flights", indexes = {
    @Index(name = "idx_flights_origin_dest_dep",
           columnList = "origin_iata, destination_iata, departure_time"),
    @Index(name = "idx_flights_number", columnList = "flight_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "flight_id", updatable = false, nullable = false)
    private UUID flightId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id")
    private Airline airline;

    @Column(name = "flight_number", nullable = false, length = 10)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_iata", referencedColumnName = "iata_code")
    private Airport origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_iata", referencedColumnName = "iata_code")
    private Airport destination;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
