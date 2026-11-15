package com.scaleup.simulation;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SimulationSettings {

    private final AtomicReference<SimulationConfig> current =
            new AtomicReference<>(SimulationConfig.defaults());

    // Increases on every update or reset, so others can tell that the settings were re-applied.
    private final AtomicLong version = new AtomicLong();

    public SimulationConfig get() {
        return current.get();
    }

    public long version() {
        return version.get();
    }

    public void update(SimulationConfig config) {
        current.set(config);
        version.incrementAndGet();
    }

    public void reset() {
        current.set(SimulationConfig.defaults());
        version.incrementAndGet();
    }
}