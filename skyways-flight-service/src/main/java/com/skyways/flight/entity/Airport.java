package com.skyways.flight.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Airport {

    @Id
    @Column(name = "iata_code", length = 3)
    private String iataCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(length = 50)
    private String timezone;
}
