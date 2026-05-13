package com.skyways.flight.repository;

import com.skyways.flight.entity.Flight;
import com.skyways.flight.entity.FlightStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlightRepository extends JpaRepository<Flight, UUID> {

    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin.iataCode = :origin
          AND f.destination.iataCode = :destination
          AND f.departureTime >= :departureFrom
          AND f.departureTime < :departureTo
          AND f.status = com.skyways.flight.entity.FlightStatus.SCHEDULED
          AND f.availableSeats >= :seatsRequired
        ORDER BY f.departureTime ASC
        """)
    Page<Flight> searchFlights(
        @Param("origin") String origin,
        @Param("destination") String destination,
        @Param("departureFrom") Instant departureFrom,
        @Param("departureTo") Instant departureTo,
        @Param("seatsRequired") int seatsRequired,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Flight f WHERE f.flightId = :flightId")
    Optional<Flight> findByIdWithLock(@Param("flightId") UUID flightId);
}
