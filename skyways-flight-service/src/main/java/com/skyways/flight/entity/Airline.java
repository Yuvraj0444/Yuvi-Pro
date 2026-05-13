package com.skyways.flight.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "airlines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "airline_id", updatable = false, nullable = false)
    private UUID airlineId;

    @Column(name = "iata_code", unique = true, length = 2)
    private String iataCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String country;
}
