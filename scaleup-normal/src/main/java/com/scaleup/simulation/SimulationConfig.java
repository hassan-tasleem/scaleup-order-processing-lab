package com.scaleup.simulation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record SimulationConfig(
        @Min(0) long paymentDelayMs,
        @DecimalMin("0.0") @DecimalMax("1.0") double paymentDeclineRate,
        @DecimalMin("0.0") @DecimalMax("1.0") double paymentTimeoutRate,
        @Min(0) long paymentTimeoutMs,
        @Min(0) long notificationDelayMs,
        @DecimalMin("0.0") @DecimalMax("1.0") double notificationFailureRate,
        long randomSeed) {

    public static SimulationConfig defaults() {
        return new SimulationConfig(0, 0.0, 0.0, 3000, 0, 0.0, 12345);
    }
}