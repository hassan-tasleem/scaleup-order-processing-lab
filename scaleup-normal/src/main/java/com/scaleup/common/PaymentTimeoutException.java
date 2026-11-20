package com.scaleup.common;

public class PaymentTimeoutException extends RuntimeException {

    public PaymentTimeoutException(Long orderId) {
        super("Payment timed out for order " + orderId);
    }
}