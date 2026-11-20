package com.scaleup.common;

public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(Long orderId) {
        super("Payment declined for order " + orderId);
    }
}