package com.scaleup.payment;

import com.scaleup.simulation.SimulationConfig;
import com.scaleup.simulation.SimulationSettings;
import com.scaleup.simulation.SimulationSupport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final SimulationSettings settings;
    private final SimulationSupport support;

    public SimulatedPaymentGateway(SimulationSettings settings, SimulationSupport support) {
        this.settings = settings;
        this.support = support;
    }

    @Override
    public PaymentOutcome charge(Long orderId, BigDecimal amount) {
        SimulationConfig config = settings.get();

        // One draw decides the outcome: first slice = timeout, next slice = declined, rest = success.
        double roll = support.nextDouble();

        if (roll < config.paymentTimeoutRate()) {
            support.sleep(config.paymentTimeoutMs());
            return PaymentOutcome.TIMEOUT;
        }

        support.sleep(config.paymentDelayMs());

        if (roll < config.paymentTimeoutRate() + config.paymentDeclineRate()) {
            return PaymentOutcome.DECLINED;
        }
        return PaymentOutcome.SUCCESS;
    }
}