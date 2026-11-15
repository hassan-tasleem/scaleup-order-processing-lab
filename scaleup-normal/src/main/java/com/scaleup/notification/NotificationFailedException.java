package com.scaleup.notification;

public class NotificationFailedException extends RuntimeException {

    public NotificationFailedException(String message) {
        super(message);
    }
}