package com.skyways.booking.exception;

import com.skyways.common.dto.ErrorDetail;
import com.skyways.common.dto.ErrorResponse;
import com.skyways.common.exception.SkyWaysBaseException;
import com.skyways.common.exception.booking.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class BookingExceptionHandler {

    private static final Logger log = LogManager.getLogger(BookingExceptionHandler.class);

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            BookingNotFoundException ex, HttpServletRequest req) {
        log.warn("BookingNotFoundException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(BookingAlreadyCancelledException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyCancelled(
            BookingAlreadyCancelledException ex, HttpServletRequest req) {
        log.warn("BookingAlreadyCancelledException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(InvalidPassengerDetailsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassenger(
            InvalidPassengerDetailsException ex, HttpServletRequest req) {
        log.warn("InvalidPassengerDetailsException [path={}]: {}", req.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(ex.getHttpStatus().value())
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(req.getRequestURI())
            .traceId(ex.getTraceId())
            .details(ex.getField() != null
                ? List.of(ErrorDetail.builder().field(ex.getField()).issue(ex.getMessage()).build())
                : null)
            .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(PassportExpiredException.class)
    public ResponseEntity<ErrorResponse> handlePassportExpired(
            PassportExpiredException ex, HttpServletRequest req) {
        log.warn("PassportExpiredException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(SagaCompensationException.class)
    public ResponseEntity<ErrorResponse> handleSagaCompensation(
            SagaCompensationException ex, HttpServletRequest req) {
        log.error("SagaCompensationException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> ErrorDetail.builder().field(fe.getField()).issue(fe.getDefaultMessage()).build())
            .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
            .status(400)
            .errorCode("VALIDATION_FAILED")
            .message("Request validation failed")
            .path(req.getRequestURI())
            .details(details)
            .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception [path={}]", req.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
            .status(500).errorCode("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred").path(req.getRequestURI()).build());
    }

    private ErrorResponse buildError(SkyWaysBaseException ex, HttpServletRequest req) {
        return ErrorResponse.builder()
            .status(ex.getHttpStatus().value())
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(req.getRequestURI())
            .traceId(ex.getTraceId())
            .build();
    }
}
