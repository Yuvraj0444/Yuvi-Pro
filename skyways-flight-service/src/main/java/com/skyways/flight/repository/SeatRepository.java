package com.skyways.flight.repository;

import com.skyways.flight.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.flight.flightId = :flightId AND s.isReserved = false")
    List<Seat> findAvailableSeatsWithLock(@Param("flightId") UUID flightId);

    long countByFlightFlightIdAndIsReservedFalse(UUID flightId);

    Optional<Seat> findByFlightFlightIdAndSeatNumber(UUID flightId, String seatNumber);

    List<Seat> findByBookingId(UUID bookingId);
}
