package com.scaleup.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A plain id, not an object link: the notification module does not depend on the order module.
    // No unique constraint on (order_id, type): it would hide duplicates, which P18 studies.
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
        // Required by JPA.
    }

    public Notification(Long orderId, String type, NotificationStatus status) {
        this.orderId = orderId;
        this.type = type;
        this.status = status;
        this.attempts = 1;
        Instant now = Instant.now();
        this.createdAt = now;
        this.sentAt = (status == NotificationStatus.SENT) ? now : null;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getType() {
        return type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}