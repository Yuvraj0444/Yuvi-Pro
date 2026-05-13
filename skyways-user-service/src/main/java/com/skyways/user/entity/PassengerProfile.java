package com.skyways.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passenger_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id", updatable = false, nullable = false)
    private UUID profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 3-DES encrypted */
    @Column(name = "passport_no", nullable = false)
    private String passportNo;

    /** 3-DES encrypted */
    @Column(name = "passport_expiry", nullable = false)
    private String passportExpiry;

    @Column(length = 100)
    private String nationality;

    /** 3-DES encrypted */
    @Column(name = "date_of_birth", nullable = false)
    private String dateOfBirth;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
