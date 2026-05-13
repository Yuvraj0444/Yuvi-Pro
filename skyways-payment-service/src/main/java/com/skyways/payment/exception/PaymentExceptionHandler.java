package com.skyways.payment.exception;

import com.skyways.common.dto.ErrorResponse;
import com.skyways.common.exception.SkyWaysBaseException;
import com.skyways.common.exception.payment.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    private static final Logger log = LogManager.getLogger(PaymentExceptionHandler.class);

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(
            PaymentFailedException ex, HttpServletRequest req) {
        log.warn("PaymentFailedException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(PaymentGatewayUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleGatewayUnavailable(
            PaymentGatewayUnavailableException ex, HttpServletRequest req) {
        log.error("PaymentGatewayUnavailableException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(InvalidPaymentAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(
            InvalidPaymentAmountException ex, HttpServletRequest req) {
        log.warn("InvalidPaymentAmountException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicatePaymentException ex, HttpServletRequest req) {
        log.warn("DuplicatePaymentException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(RefundFailedException.class)
    public ResponseEntity<ErrorResponse> handleRefundFailed(
            RefundFailedException ex, HttpServletRequest req) {
        log.error("RefundFailedException [path={}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(buildError(ex, req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled payment exception [path={}]", req.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
            .status(500).errorCode("INTERNAL_SERVER_ERROR")
            .message("An unexpected payment error occurred").path(req.getRequestURI()).build());
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
