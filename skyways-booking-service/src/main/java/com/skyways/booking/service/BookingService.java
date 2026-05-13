package com.skyways.booking.service;

import com.skyways.booking.dto.CreateBookingRequest;
import com.skyways.booking.dto.PassengerDto;
import com.skyways.booking.entity.*;
import com.skyways.booking.kafka.BookingEventProducer;
import com.skyways.booking.repository.BookingRepository;
import com.skyways.booking.repository.BookingStatusHistoryRepository;
import com.skyways.common.exception.booking.BookingAlreadyCancelledException;
import com.skyways.common.exception.booking.BookingNotFoundException;
import com.skyways.common.security.TripleDESEncryptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingService {

    private static final Logger log = LogManager.getLogger(BookingService.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final PassengerValidationService validationService;
    private final BookingEventProducer eventProducer;
    private final TripleDESEncryptor encryptor;

    public BookingService(BookingRepository bookingRepository,
                          BookingStatusHistoryRepository historyRepository,
                          PassengerValidationService validationService,
                          BookingEventProducer eventProducer,
                          TripleDESEncryptor encryptor) {
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
        this.eventProducer = eventProducer;
        this.encryptor = encryptor;
    }

    @Transactional
    public Booking createBooking(CreateBookingRequest req, UUID userId) {
        try {
            req.getPassengers().forEach(validationService::validate);

            UUID sagaId = UUID.randomUUID();

            Booking booking = Booking.builder()
                .userId(userId)
                .bookingRef(generateBookingRef())
                .totalAmount(req.getTotalAmount())
                .currency(req.getCurrency())
                .status(BookingStatus.INITIATED)
                .sagaId(sagaId)
                .build();

            List<Passenger> passengers = req.getPassengers().stream()
                .map(p -> buildEncryptedPassenger(p, booking))
                .toList();
            booking.setPassengers(passengers);

            BookingItem item = BookingItem.builder()
                .booking(booking)
                .flightId(req.getFlightId())
                .fareClass(req.getFareClass())
                .price(req.getTotalAmount())
                .build();
            booking.setItems(List.of(item));

            Booking saved = bookingRepository.save(booking);

            recordStatusChange(saved.getBookingId(), null, BookingStatus.INITIATED, "Booking created");

            log.info("Booking created [bookingId={}, bookingRef={}, sagaId={}, traceId={}]",
                saved.getBookingId(), saved.getBookingRef(), sagaId, ThreadContext.get("traceId"));

            eventProducer.publishBookingInitiated(saved, req.getFlightId(), req.getPassengers().size());

            return saved;

        } catch (com.skyways.common.exception.SkyWaysBaseException e) {
            log.warn("Booking creation rejected [traceId={}]: {}", ThreadContext.get("traceId"), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected booking creation error [traceId={}]", ThreadContext.get("traceId"), e);
            throw e;
        }
    }

    @Transactional
    public void updateBookingStatus(UUID bookingId, BookingStatus newStatus, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException(booking.getBookingRef());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        bookingRepository.save(booking);

        recordStatusChange(bookingId, oldStatus, newStatus, reason);

        log.info("Booking status updated [bookingId={}, {}->{}]",
            bookingId, oldStatus, newStatus);
    }

    @Transactional(readOnly = true)
    public Booking getByRef(String bookingRef) {
        return bookingRepository.findByBookingRef(bookingRef)
            .orElseThrow(() -> new BookingNotFoundException(bookingRef));
    }

    private Passenger buildEncryptedPassenger(PassengerDto dto, Booking booking) {
        return Passenger.builder()
            .booking(booking)
            .firstName(encryptor.encrypt(dto.getFirstName()))
            .lastName(encryptor.encrypt(dto.getLastName()))
            .passportNo(encryptor.encrypt(dto.getPassportNo()))
            .nationality(dto.getNationality())
            .dateOfBirth(encryptor.encrypt(dto.getDateOfBirth()))
            .email(encryptor.encrypt(dto.getEmail()))
            .phone(dto.getPhone() != null ? encryptor.encrypt(dto.getPhone()) : null)
            .build();
    }

    private void recordStatusChange(UUID bookingId, BookingStatus oldStatus,
                                     BookingStatus newStatus, String reason) {
        historyRepository.save(BookingStatusHistory.builder()
            .bookingId(bookingId)
            .oldStatus(oldStatus != null ? oldStatus.name() : null)
            .newStatus(newStatus.name())
            .reason(reason)
            .build());
    }

    private String generateBookingRef() {
        StringBuilder sb = new StringBuilder("SW-");
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        String ref = sb.toString();
        return bookingRepository.findByBookingRef(ref).isPresent() ? generateBookingRef() : ref;
    }
}
