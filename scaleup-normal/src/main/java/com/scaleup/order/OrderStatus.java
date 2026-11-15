package com.scaleup.order;

public enum OrderStatus {
    PENDING,         // accepted but not processed yet (asynchronous versions)
    STOCK_RESERVED,  // stock is reserved, payment not done yet
    PAID,            // payment succeeded, notification not done yet
    CONFIRMED,       // everything succeeded
    PAYMENT_FAILED,  // payment declined, stock released
    CANCELLED        // reserved for later waves
}