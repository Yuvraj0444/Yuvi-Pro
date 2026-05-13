package com.skyways.common.enums;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String BOOKING_INITIATED              = "booking-initiated";
    public static final String BOOKING_CONFIRMED              = "booking-confirmed";
    public static final String BOOKING_CANCELLED              = "booking-cancelled";

    public static final String SEAT_RESERVATION_REQUESTED    = "seat-reservation-requested";
    public static final String SEAT_RESERVATION_CONFIRMED    = "seat-reservation-confirmed";
    public static final String SEAT_RESERVATION_FAILED       = "seat-reservation-failed";
    public static final String SEAT_RELEASE_REQUESTED        = "seat-release-requested";

    public static final String PAYMENT_INITIATION_REQUESTED  = "payment-initiation-requested";
    public static final String PAYMENT_PROCESSED             = "payment-processed";
    public static final String PAYMENT_FAILED                = "payment-failed";

    public static final String NOTIFICATION_REQUESTED        = "notification-requested";
    public static final String NOTIFICATION_DLQ              = "notification-dlq";

    public static final String SAGA_EVENTS                   = "saga-events";
    public static final String USER_EVENTS                   = "user-events";
    public static final String FLIGHT_STATUS_CHANGED         = "flight-status-changed";
}
