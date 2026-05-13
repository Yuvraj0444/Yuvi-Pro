package com.skyways.flight.service;

import com.skyways.common.exception.flight.FlightNotFoundException;
import com.skyways.common.exception.flight.FlightOverBookedException;
import com.skyways.common.exception.flight.SeatAlreadyReservedException;
import com.skyways.flight.entity.Flight;
import com.skyways.flight.entity.Seat;
import com.skyways.flight.repository.FlightRepository;
import com.skyways.flight.repository.SeatRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SeatService {

    private static final Logger log = LogManager.getLogger(SeatService.class);

    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;

    public SeatService(SeatRepository seatRepository, FlightRepository flightRepository) {
        this.seatRepository = seatRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Atomically reserves seats for a booking using a pessimistic write lock
     * on the flight row. Prevents overbooking under concurrent load.
     */
    @Transactional
    public List<Seat> reserveSeats(UUID flightId, UUID bookingId, int seatCount) {
        try {
            Flight flight = flightRepository.findByIdWithLock(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId.toString()));

            if (flight.getAvailableSeats() < seatCount) {
                throw new FlightOverBookedException(
                    flightId.toString(), seatCount, flight.getAvailableSeats());
            }

            List<Seat> availableSeats =
                seatRepository.findAvailableSeatsWithLock(flightId);

            if (availableSeats.size() < seatCount) {
                throw new FlightOverBookedException(
                    flightId.toString(), seatCount, availableSeats.size());
            }

            List<Seat> seatsToReserve = availableSeats.subList(0, seatCount);
            seatsToReserve.forEach(seat -> {
                seat.setReserved(true);
                seat.setReservedAt(Instant.now());
                seat.setBookingId(bookingId);
            });

            List<Seat> reserved = seatRepository.saveAll(seatsToReserve);

            flight.setAvailableSeats(flight.getAvailableSeats() - seatCount);
            flightRepository.save(flight);

            log.info("Reserved {} seats on flight {} for booking {}",
                seatCount, flightId, bookingId);

            return reserved;

        } catch (FlightNotFoundException | FlightOverBookedException e) {
            log.warn("Seat reservation failed [flightId={}]: {}", flightId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error reserving seats [flightId={}, bookingId={}]",
                flightId, bookingId, e);
            throw e;
        }
    }

    @Transactional
    public void releaseSeats(UUID bookingId) {
        try {
            List<Seat> seats = seatRepository.findByBookingId(bookingId);

            if (seats.isEmpty()) {
                log.warn("No seats found for booking {} during release", bookingId);
                return;
            }

            UUID flightId = seats.get(0).getFlight().getFlightId();
            seats.forEach(seat -> {
                seat.setReserved(false);
                seat.setReservedAt(null);
                seat.setBookingId(null);
            });
            seatRepository.saveAll(seats);

            Flight flight = flightRepository.findByIdWithLock(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId.toString()));
            flight.setAvailableSeats(flight.getAvailableSeats() + seats.size());
            flightRepository.save(flight);

            log.info("Released {} seats for booking {}", seats.size(), bookingId);

        } catch (Exception e) {
            log.error("Error releasing seats for booking {}", bookingId, e);
            throw e;
        }
    }
}
