package com.scaleup.notification;

public interface NotificationSender {

    // Returns normally if the notification was sent; throws NotificationFailedException if not.
    void send(Long orderId, String type);
}