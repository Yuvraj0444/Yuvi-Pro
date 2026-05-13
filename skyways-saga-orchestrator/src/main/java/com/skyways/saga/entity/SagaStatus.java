package com.skyways.saga.entity;

public enum SagaStatus {
    STARTED,
    SEAT_RESERVATION_PENDING,
    SEAT_RESERVED,
    PAYMENT_PENDING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
