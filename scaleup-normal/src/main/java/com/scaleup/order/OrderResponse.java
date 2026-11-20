package com.scaleup.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant confirmedAt) {

    static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getConfirmedAt());
    }
}