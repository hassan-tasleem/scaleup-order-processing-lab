package com.scaleup.common;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requestedQuantity) {
        super("Not enough stock for product " + productId + " (requested " + requestedQuantity + ")");
    }
}