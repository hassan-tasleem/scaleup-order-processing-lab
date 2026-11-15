package com.scaleup.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, String name, String category,
                              BigDecimal price, boolean active, int availableQuantity) {

    static ProductResponse from(Product product, int availableQuantity) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.isActive(),
                availableQuantity);
    }
}