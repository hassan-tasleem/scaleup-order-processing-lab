package com.scaleup.notification;

import com.scaleup.simulation.SimulationConfig;
import com.scaleup.simulation.SimulationSettings;
import com.scaleup.simulation.SimulationSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedNotificationSenderTest {

    private final SimulationSettings settings = new SimulationSettings();
    private final SimulatedNotificationSender sender =
            new SimulatedNotificationSender(settings, new SimulationSupport(settings));

    private static SimulationConfig config(long delayMs, double failureRate) {
        return new SimulationConfig(0, 0.0, 0.0, 3000, delayMs, failureRate, 12345);
    }

    // true = the call failed, false = the call worked
    private List<Boolean> sendMany(int times) {
        List<Boolean> failed = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            try {
                sender.send(1L, "ORDER_CONFIRMATION");
                failed.add(false);
            } catch (NotificationFailedException e) {
                failed.add(true);
            }
        }
        return failed;
    }

    @Test
    void defaultSettingsNeverFail() {
        assertThat(sendMany(100)).containsOnly(false);
    }

    @Test
    void delayIsApplied() {
        settings.update(config(200, 0.0));

        long start = System.nanoTime();
        sender.send(1L, "ORDER_CONFIRMATION");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // A little below 200 is allowed because timers are not perfectly exact.
        assertThat(elapsedMs).isGreaterThanOrEqualTo(190);
    }

    @Test
    void failureRateOfOneAlwaysFails() {
        settings.update(config(0, 1.0));

        assertThatThrownBy(() -> sender.send(1L, "ORDER_CONFIRMATION"))
                .isInstanceOf(NotificationFailedException.class)
                .hasMessageContaining("order 1");
    }

    @Test
    void aFailingCallStillTakesTheDelay() {
        settings.update(config(200, 1.0));

        long start = System.nanoTime();
        assertThatThrownBy(() -> sender.send(1L, "ORDER_CONFIRMATION"))
                .isInstanceOf(NotificationFailedException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isGreaterThanOrEqualTo(190);
    }

    @Test
    void sameSettingsGiveTheSameSequence() {
        settings.update(config(0, 0.5));
        List<Boolean> first = sendMany(20);

        // Applying the settings again restarts the random sequence.
        settings.update(config(0, 0.5));
        List<Boolean> second = sendMany(20);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void observedFailureShareIsCloseToTheConfiguredRate() {
        settings.update(config(0, 0.3));

        long failures = sendMany(10_000).stream().filter(failed -> failed).count();

        assertThat(failures).isBetween(2_700L, 3_300L);
    }
}