package com.skyways.common.exception.notification;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class NotificationDeliveryException extends SkyWaysBaseException {

    public NotificationDeliveryException(String message) {
        super("NOTIFICATION_DELIVERY_FAILED", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super("NOTIFICATION_DELIVERY_FAILED", message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
