package com.skyways.booking.repository;

import com.skyways.booking.entity.Booking;
import com.skyways.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByBookingRef(String bookingRef);
    Page<Booking> findByUserId(UUID userId, Pageable pageable);
    Optional<Booking> findBySagaId(UUID sagaId);
    long countByUserIdAndStatus(UUID userId, BookingStatus status);
}
