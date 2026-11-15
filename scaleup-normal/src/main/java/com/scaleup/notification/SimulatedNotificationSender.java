package com.scaleup.notification;

import com.scaleup.simulation.SimulationConfig;
import com.scaleup.simulation.SimulationSettings;
import com.scaleup.simulation.SimulationSupport;
import org.springframework.stereotype.Component;

@Component
public class SimulatedNotificationSender implements NotificationSender {

    private final SimulationSettings settings;
    private final SimulationSupport support;

    public SimulatedNotificationSender(SimulationSettings settings, SimulationSupport support) {
        this.settings = settings;
        this.support = support;
    }

    @Override
    public void send(Long orderId, String type) {
        SimulationConfig config = settings.get();
        double roll = support.nextDouble();

        // A failing call still takes its time before the error comes back.
        support.sleep(config.notificationDelayMs());

        if (roll < config.notificationFailureRate()) {
            throw new NotificationFailedException(
                    "Simulated notification failure for order " + orderId);
        }
    }
}