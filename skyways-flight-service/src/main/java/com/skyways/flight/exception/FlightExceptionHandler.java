package com.skyways.flight.exception;

import com.skyways.common.dto.ErrorResponse;
import com.skyways.common.exception.SkyWaysBaseException;
import com.skyways.common.exception.flight.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FlightExceptionHandler {

    private static final Logger log = LogManager.getLogger(FlightExceptionHandler.class);

    @ExceptionHandler(FlightNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFlightNotFound(
            FlightNotFoundException ex, HttpServletRequest request) {
        log.warn("FlightNotFoundException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, request));
    }

    @ExceptionHandler(FlightOverBookedException.class)
    public ResponseEntity<ErrorResponse> handleOverBooked(
            FlightOverBookedException ex, HttpServletRequest request) {
        log.warn("FlightOverBookedException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, request));
    }

    @ExceptionHandler(SeatAlreadyReservedException.class)
    public ResponseEntity<ErrorResponse> handleSeatReserved(
            SeatAlreadyReservedException ex, HttpServletRequest request) {
        log.warn("SeatAlreadyReservedException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, request));
    }

    @ExceptionHandler(GDSConnectionException.class)
    public ResponseEntity<ErrorResponse> handleGDSConnection(
            GDSConnectionException ex, HttpServletRequest request) {
        log.error("GDSConnectionException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, request));
    }

    @ExceptionHandler(SkyscannerAPIException.class)
    public ResponseEntity<ErrorResponse> handleSkyscanner(
            SkyscannerAPIException ex, HttpServletRequest request) {
        log.error("SkyscannerAPIException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [path={}]", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
            .status(500)
            .errorCode("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .build());
    }

    private ErrorResponse buildError(SkyWaysBaseException ex, HttpServletRequest request) {
        return ErrorResponse.builder()
            .status(ex.getHttpStatus().value())
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .traceId(ex.getTraceId())
            .build();
    }
}
