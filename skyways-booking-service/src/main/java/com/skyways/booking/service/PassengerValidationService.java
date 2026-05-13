package com.skyways.booking.service;

import com.skyways.common.exception.booking.InvalidContactInfoException;
import com.skyways.common.exception.booking.InvalidPassengerDetailsException;
import com.skyways.common.exception.booking.PassportExpiredException;
import com.skyways.booking.dto.PassengerDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Service
public class PassengerValidationService {

    private static final Logger log = LogManager.getLogger(PassengerValidationService.class);
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Z0-9]{6,9}$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public void validate(PassengerDto passenger) {
        try {
            validatePassportNumber(passenger.getPassportNo());
            validatePassportExpiry(passenger.getPassportExpiry());
            validateDateOfBirth(passenger.getDateOfBirth());
            validateEmail(passenger.getEmail());
            validateName(passenger.getFirstName(), "firstName");
            validateName(passenger.getLastName(), "lastName");

            log.debug("Passenger validation passed [traceId={}] for passport {}",
                ThreadContext.get("traceId"), maskPassport(passenger.getPassportNo()));

        } catch (InvalidPassengerDetailsException e) {
            log.warn("Passenger validation failed [traceId={}]: {}",
                ThreadContext.get("traceId"), e.getMessage());
            throw e;
        }
    }

    private void validatePassportNumber(String passportNo) {
        if (passportNo == null || !PASSPORT_PATTERN.matcher(passportNo).matches()) {
            throw new InvalidPassengerDetailsException(
                "Passport number must be 6-9 alphanumeric characters (uppercase)",
                "passportNo");
        }
    }

    private void validatePassportExpiry(String expiryStr) {
        try {
            LocalDate expiry = LocalDate.parse(expiryStr);
            if (expiry.isBefore(LocalDate.now().plusMonths(6))) {
                throw new PassportExpiredException(expiryStr);
            }
        } catch (DateTimeParseException e) {
            throw new InvalidPassengerDetailsException(
                "Passport expiry must be in YYYY-MM-DD format", "passportExpiry");
        }
    }

    private void validateDateOfBirth(String dobStr) {
        try {
            LocalDate dob = LocalDate.parse(dobStr);
            if (dob.isAfter(LocalDate.now().minusYears(2))) {
                throw new InvalidPassengerDetailsException(
                    "Passenger must be at least 2 years old", "dateOfBirth");
            }
            if (dob.isBefore(LocalDate.now().minusYears(120))) {
                throw new InvalidPassengerDetailsException(
                    "Date of birth is not plausible", "dateOfBirth");
            }
        } catch (DateTimeParseException e) {
            throw new InvalidPassengerDetailsException(
                "Date of birth must be in YYYY-MM-DD format", "dateOfBirth");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidContactInfoException("Invalid email address format", "email");
        }
    }

    private void validateName(String name, String field) {
        if (name == null || name.isBlank() || name.length() < 2) {
            throw new InvalidPassengerDetailsException(
                field + " must be at least 2 characters", field);
        }
        if (name.matches(".*[<>\"'%;()&+].*")) {
            throw new InvalidPassengerDetailsException(
                field + " contains invalid characters", field);
        }
    }

    private String maskPassport(String passport) {
        if (passport == null || passport.length() < 4) return "****";
        return "****" + passport.substring(passport.length() - 3);
    }
}
