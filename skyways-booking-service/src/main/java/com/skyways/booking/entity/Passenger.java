package com.skyways.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "passengers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "passenger_id", updatable = false, nullable = false)
    private UUID passengerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** 3-DES encrypted */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /** 3-DES encrypted */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** 3-DES encrypted */
    @Column(name = "passport_no", nullable = false)
    private String passportNo;

    @Column(length = 100)
    private String nationality;

    /** 3-DES encrypted */
    @Column(name = "date_of_birth", nullable = false)
    private String dateOfBirth;

    /** 3-DES encrypted */
    @Column
    private String email;

    /** 3-DES encrypted */
    @Column
    private String phone;
}
