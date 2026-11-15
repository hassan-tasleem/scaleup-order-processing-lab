package com.scaleup.admin;

public enum SeedProfile {

    SMALL(100, 500),
    STANDARD(10_000, 5_000);

    private final int customers;
    private final int products;

    SeedProfile(int customers, int products) {
        this.customers = customers;
        this.products = products;
    }

    public int getCustomers() {
        return customers;
    }

    public int getProducts() {
        return products;
    }
}