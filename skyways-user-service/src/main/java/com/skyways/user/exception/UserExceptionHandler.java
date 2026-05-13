package com.skyways.user.exception;

import com.skyways.common.dto.ErrorDetail;
import com.skyways.common.dto.ErrorResponse;
import com.skyways.common.exception.SkyWaysBaseException;
import com.skyways.common.exception.auth.AuthenticationException;
import com.skyways.common.exception.auth.TokenExpiredException;
import com.skyways.common.exception.auth.UnauthorizedAccessException;
import com.skyways.common.exception.booking.InvalidPassengerDetailsException;
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
public class UserExceptionHandler {

    private static final Logger log = LogManager.getLogger(UserExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("AuthenticationException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
            .body(buildError(ex, request.getRequestURI()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(
            TokenExpiredException ex, HttpServletRequest request) {
        log.warn("TokenExpiredException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
            .body(buildError(ex, request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, HttpServletRequest request) {
        log.warn("UnauthorizedAccessException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
            .body(buildError(ex, request.getRequestURI()));
    }

    @ExceptionHandler(InvalidPassengerDetailsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassengerDetails(
            InvalidPassengerDetailsException ex, HttpServletRequest request) {
        log.warn("InvalidPassengerDetailsException [path={}]: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = buildError(ex, request.getRequestURI());
        if (ex.getField() != null) {
            error = ErrorResponse.builder()
                .status(ex.getHttpStatus().value())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(ex.getTraceId())
                .details(List.of(ErrorDetail.builder().field(ex.getField()).issue(ex.getMessage()).build()))
                .build();
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> ErrorDetail.builder().field(fe.getField()).issue(fe.getDefaultMessage()).build())
            .collect(Collectors.toList());

        log.warn("Validation failed [path={}]: {} field errors", request.getRequestURI(), details.size());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
            .status(400)
            .errorCode("VALIDATION_FAILED")
            .message("Request validation failed")
            .path(request.getRequestURI())
            .details(details)
            .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [path={}]", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
            .status(500)
            .errorCode("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred. Our team has been notified.")
            .path(request.getRequestURI())
            .build());
    }

    private ErrorResponse buildError(SkyWaysBaseException ex, String path) {
        return ErrorResponse.builder()
            .status(ex.getHttpStatus().value())
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(path)
            .traceId(ex.getTraceId())
            .build();
    }
}
