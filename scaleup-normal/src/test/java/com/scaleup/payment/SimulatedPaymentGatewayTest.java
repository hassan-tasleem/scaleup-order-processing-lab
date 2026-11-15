package com.scaleup.payment;

import com.scaleup.simulation.SimulationConfig;
import com.scaleup.simulation.SimulationSettings;
import com.scaleup.simulation.SimulationSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    private final SimulationSettings settings = new SimulationSettings();
    private final SimulatedPaymentGateway gateway =
            new SimulatedPaymentGateway(settings, new SimulationSupport(settings));

    private static SimulationConfig config(long delayMs, double declineRate, double timeoutRate, long timeoutMs) {
        return new SimulationConfig(delayMs, declineRate, timeoutRate, timeoutMs, 0, 0.0, 12345);
    }

    private List<PaymentOutcome> charge(int times) {
        List<PaymentOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            outcomes.add(gateway.charge(1L, BigDecimal.TEN));
        }
        return outcomes;
    }

    @Test
    void defaultSettingsAlwaysSucceed() {
        assertThat(charge(100)).containsOnly(PaymentOutcome.SUCCESS);
    }

    @Test
    void delayIsApplied() {
        settings.update(config(200, 0.0, 0.0, 3000));

        long start = System.nanoTime();
        PaymentOutcome outcome = gateway.charge(1L, BigDecimal.TEN);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(outcome).isEqualTo(PaymentOutcome.SUCCESS);
        // A little below 200 is allowed because timers are not perfectly exact.
        assertThat(elapsedMs).isGreaterThanOrEqualTo(190);
    }

    @Test
    void declineRateOfOneAlwaysDeclines() {
        settings.update(config(0, 1.0, 0.0, 3000));

        assertThat(charge(20)).containsOnly(PaymentOutcome.DECLINED);
    }

    @Test
    void timeoutRateOfOneBlocksThenTimesOut() {
        settings.update(config(0, 0.0, 1.0, 300));

        long start = System.nanoTime();
        PaymentOutcome outcome = gateway.charge(1L, BigDecimal.TEN);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(outcome).isEqualTo(PaymentOutcome.TIMEOUT);
        assertThat(elapsedMs).isGreaterThanOrEqualTo(290);
    }

    @Test
    void sameSettingsGiveTheSameSequence() {
        settings.update(config(0, 0.5, 0.0, 3000));
        List<PaymentOutcome> first = charge(20);

        // Applying the settings again restarts the random sequence.
        settings.update(config(0, 0.5, 0.0, 3000));
        List<PaymentOutcome> second = charge(20);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void observedDeclineShareIsCloseToTheConfiguredRate() {
        settings.update(config(0, 0.3, 0.0, 3000));

        long declined = charge(10_000).stream()
                .filter(outcome -> outcome == PaymentOutcome.DECLINED)
                .count();

        assertThat(declined).isBetween(2_700L, 3_300L);
    }
}